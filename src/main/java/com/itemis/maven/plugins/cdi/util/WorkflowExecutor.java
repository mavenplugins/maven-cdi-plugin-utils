package com.itemis.maven.plugins.cdi.util;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.itemis.maven.plugins.cdi.CDIMojoProcessingStep;
import com.itemis.maven.plugins.cdi.annotations.RollbackOnError;
import com.itemis.maven.plugins.cdi.workflow.ProcessingWorkflow;
import com.itemis.maven.plugins.cdi.workflow.WorkflowStep;

public class WorkflowExecutor {
  private Log log;
  private ProcessingWorkflow workflow;
  private Map<String, CDIMojoProcessingStep> processingSteps;
  private Stack<CDIMojoProcessingStep> executedSteps;

  public WorkflowExecutor(ProcessingWorkflow workflow, Map<String, CDIMojoProcessingStep> processingSteps, Log log) {
    this.workflow = workflow;
    this.processingSteps = processingSteps;
    this.log = log;
  }

  public void validate() throws MojoExecutionException {
    Set<String> unknownIds = Sets.newHashSet();
    for (WorkflowStep workflowStep : this.workflow.getProcessingSteps()) {
      if (workflowStep.isParallel()) {
        for (String id : workflowStep.getParallelStepIds()) {
          if (!this.processingSteps.containsKey(id)) {
            unknownIds.add(id);
          }
        }
      } else {
        if (!this.processingSteps.containsKey(workflowStep.getStepId())) {
          unknownIds.add(workflowStep.getStepId());
        }
      }
    }

    if (!unknownIds.isEmpty()) {
      throw new MojoExecutionException(
          "There are no implementations for the following processing step ids specified in the workflow: "
              + Joiner.on(',').join(unknownIds));
    }
  }

  public void execute() throws MojoExecutionException, MojoFailureException {
    this.executedSteps = new Stack<CDIMojoProcessingStep>();

    for (WorkflowStep workflowStep : this.workflow.getProcessingSteps()) {
      executeSequencialWorkflowStep(workflowStep);
      executeParallelWorkflowSteps(workflowStep);
    }
  }

  private void executeSequencialWorkflowStep(WorkflowStep workflowStep)
      throws MojoExecutionException, MojoFailureException {
    if (workflowStep.isParallel()) {
      return;
    }

    CDIMojoProcessingStep step = this.processingSteps.get(workflowStep.getStepId());
    try {
      this.executedSteps.push(step);
      step.execute();
    } catch (Throwable t) {
      rollback(t);
      // throw original exception after rollback!
      if (t instanceof MojoExecutionException) {
        throw (MojoExecutionException) t;
      } else if (t instanceof MojoFailureException) {
        throw (MojoFailureException) t;
      } else if (t instanceof RuntimeException) {
        throw (RuntimeException) t;
      } else {
        throw new RuntimeException(t);
      }
    }
  }

  private void executeParallelWorkflowSteps(WorkflowStep workflowStep)
      throws MojoExecutionException, MojoFailureException {
    if (!workflowStep.isParallel()) {
      return;
    }

    Queue<Future<?>> results = new LinkedList<Future<?>>();
    final Collection<Throwable> thrownExceptions = Lists.newArrayList();

    ExecutorService executorService = Executors.newFixedThreadPool(workflowStep.getParallelStepIds().size());
    for (final String stepId : workflowStep.getParallelStepIds()) {
      results.offer(executorService.submit(new Runnable() {
        @Override
        public void run() {
          CDIMojoProcessingStep step = WorkflowExecutor.this.processingSteps.get(stepId);
          try {
            WorkflowExecutor.this.executedSteps.push(step);
            step.execute();
          } catch (Throwable t) {
            thrownExceptions.add(t);
          }
        }
      }));
    }

    while (!results.isEmpty()) {
      Future<?> result = results.poll();
      try {
        result.get();
      } catch (InterruptedException e) {
        results.offer(result);
      } catch (ExecutionException e) {
        // do nothing since this exception will be handled later!
      }
    }

    Throwable firstError = Iterables.getFirst(thrownExceptions, null);
    if (firstError != null) {
      rollback(firstError);
      // throw original exception after rollback!
      if (firstError instanceof MojoExecutionException) {
        throw (MojoExecutionException) firstError;
      } else if (firstError instanceof MojoFailureException) {
        throw (MojoFailureException) firstError;
      } else if (firstError instanceof RuntimeException) {
        throw (RuntimeException) firstError;
      } else {
        throw new RuntimeException(firstError);
      }
    }
  }

  private void rollback(Throwable t) {
    while (!this.executedSteps.empty()) {
      rollback(this.executedSteps.pop(), t);
    }
  }

  private void rollback(CDIMojoProcessingStep step, Throwable t) {
    // get rollback methods and sort alphabetically
    List<Method> rollbackMethods = getRollbackMethods(step, t.getClass());
    rollbackMethods.sort(new Comparator<Method>() {
      @Override
      public int compare(Method m1, Method m2) {
        return m1.getName().compareTo(m2.getName());
      }
    });

    // call rollback methods
    for (Method rollbackMethod : rollbackMethods) {
      rollbackMethod.setAccessible(true);
      try {
        if (rollbackMethod.getParameters().length == 1) {
          rollbackMethod.invoke(step, t);
        } else {
          rollbackMethod.invoke(step);
        }
      } catch (ReflectiveOperationException e) {
        this.log.error("Error calling rollback method of Mojo.", e);
      }
    }
  }

  private <T extends Throwable> List<Method> getRollbackMethods(CDIMojoProcessingStep mojo, Class<T> causeType) {
    List<Method> rollbackMethods = Lists.newArrayList();
    for (Method m : mojo.getClass().getDeclaredMethods()) {
      RollbackOnError rollbackAnnotation = m.getAnnotation(RollbackOnError.class);
      if (rollbackAnnotation != null) {
        boolean considerMethod = false;

        // consider method for inclusion if no error types are declared or if one of the declared error types is a
        // supertype of the caught exception
        Class<? extends Throwable>[] errorTypes = rollbackAnnotation.value();
        if (errorTypes.length == 0) {
          considerMethod = true;
        } else {
          for (Class<? extends Throwable> errorType : errorTypes) {
            if (errorType.isAssignableFrom(causeType)) {
              considerMethod = true;
              break;
            }
          }
        }

        // now check also the method parameters (0 or one exception type)
        if (considerMethod) {
          Class<?>[] parameterTypes = m.getParameterTypes();
          switch (parameterTypes.length) {
            case 0:
              rollbackMethods.add(m);
              break;
            case 1:
              if (parameterTypes[0].isAssignableFrom(causeType)) {
                rollbackMethods.add(m);
              }
              break;
            default:
              this.log.warn(
                  "Found rollback method with more than one parameters! Only zero or one parameter of type <T extends Throwable> is allowed!");
              break;
          }
        }
      }
    }

    return rollbackMethods;
  }
}
