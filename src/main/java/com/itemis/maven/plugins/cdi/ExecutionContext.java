package com.itemis.maven.plugins.cdi;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.maven.project.MavenProject;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class ExecutionContext {
  public static final String PROJ_VAR_VERSION = "@{project.version}";
  public static final String PROJ_VAR_GID = "@{project.groupId}";
  public static final String PROJ_VAR_AID = "@{project.artifactId}";

  private String stepId;
  private String stepQualifier;
  private Map<String, String> mappedData;
  private Iterable<String> unmappedData;
  private boolean variablesExpanded;
  private Map<String, String> mappedRollbackData;
  private Iterable<String> unmappedRollbackData;

  private ExecutionContext(String stepId, String qualifier, Map<String, String> mappedData,
      Iterable<String> unmappedData, Map<String, String> mappedRollbackData, Iterable<String> unmappedRollbackData) {
    this.stepId = stepId;
    this.stepQualifier = qualifier;
    this.mappedData = mappedData;
    this.unmappedData = unmappedData;
    this.mappedRollbackData = mappedRollbackData;
    this.unmappedRollbackData = unmappedRollbackData;
  }

  public static Builder builder(String stepId) {
    return new Builder(stepId);
  }

  public String getStepId() {
    return this.stepId;
  }

  public String getQualifier() {
    return this.stepQualifier;
  }

  public String getCompositeStepId() {
    return this.stepId + (this.stepQualifier != null ? "[" + this.stepQualifier + "]" : "");
  }

  public boolean hasMappedData() {
    return !this.mappedData.isEmpty();
  }

  public boolean hasUnmappedData() {
    return !Iterables.isEmpty(this.unmappedData);
  }

  public boolean hasMappedRollbackData() {
    return !this.mappedRollbackData.isEmpty();
  }

  public boolean hasUnmappedRollbackData() {
    return !Iterables.isEmpty(this.unmappedRollbackData);
  }

  public Set<String> getMappedDataKeys() {
    return this.mappedData.keySet();
  }

  public String getMappedDate(String key) {
    return this.mappedData.get(key);
  }

  public boolean containsMappedDate(String key) {
    return this.mappedData.containsKey(key);
  }

  public Iterable<String> getUnmappedData() {
    return this.unmappedData;
  }

  public Set<String> getMappedRollbackDataKeys() {
    return this.mappedRollbackData.keySet();
  }

  public String getMappedRollbackDate(String key) {
    return this.mappedRollbackData.get(key);
  }

  public boolean containsMappedRollbackDate(String key) {
    return this.mappedRollbackData.containsKey(key);
  }

  public Iterable<String> getUnmappedRollbackData() {
    return this.unmappedRollbackData;
  }

  public void expandProjectVariables(MavenProject project) {
    if (this.variablesExpanded) {
      return;
    }
    expandUnmappedData(project);
    expandMappedData(project);
    this.variablesExpanded = true;
  }

  private void expandUnmappedData(MavenProject project) {
    if (hasUnmappedData()) {
      List<String> newData = Lists.newArrayList();
      for (String date : this.unmappedData) {
        newData.add(expand(date, project));
      }
      this.unmappedData = Iterables.unmodifiableIterable(newData);
    }

    if (hasUnmappedRollbackData()) {
      List<String> newData = Lists.newArrayList();
      for (String date : this.unmappedRollbackData) {
        newData.add(expand(date, project));
      }
      this.unmappedRollbackData = Iterables.unmodifiableIterable(newData);
    }
  }

  private void expandMappedData(MavenProject project) {
    if (hasMappedData()) {
      Map<String, String> newData = Maps.newHashMap();
      for (Entry<String, String> entry : this.mappedData.entrySet()) {
        newData.put(entry.getKey(), expand(entry.getValue(), project));
      }
      this.mappedData = Collections.unmodifiableMap(this.mappedData);
    }
    if (hasMappedRollbackData()) {
      Map<String, String> newData = Maps.newHashMap();
      for (Entry<String, String> entry : this.mappedRollbackData.entrySet()) {
        newData.put(entry.getKey(), expand(entry.getValue(), project));
      }
      this.mappedRollbackData = Collections.unmodifiableMap(this.mappedRollbackData);
    }
  }

  private String expand(String s, MavenProject project) {
    return s.replace(PROJ_VAR_GID, project.getGroupId()).replace(PROJ_VAR_AID, project.getArtifactId())
        .replace(PROJ_VAR_VERSION, project.getVersion());
  }

  public static class Builder {
    private String id;
    private String qualifier;
    private List<String> unmappedData;
    private Map<String, String> mappedData;
    private List<String> unmappedRollbackData;
    private Map<String, String> mappedRollbackData;

    public Builder(String stepId) {
      this.id = stepId;
      this.mappedData = Maps.newHashMap();
      this.unmappedData = Lists.newArrayList();
      this.mappedRollbackData = Maps.newHashMap();
      this.unmappedRollbackData = Lists.newArrayList();
    }

    public Builder setQualifier(String qualifier) {
      this.qualifier = qualifier;
      return this;
    }

    public Builder addData(String... data) {
      for (String date : data) {
        this.unmappedData.add(date);
      }
      return this;
    }

    public Builder addRollbackData(String... data) {
      for (String date : data) {
        this.unmappedRollbackData.add(date);
      }
      return this;
    }

    public Builder addData(String key, String value) {
      this.mappedData.put(key, value);
      return this;
    }

    public Builder addRollbackData(String key, String value) {
      this.mappedRollbackData.put(key, value);
      return this;
    }

    public ExecutionContext build() {
      return new ExecutionContext(this.id, this.qualifier, Collections.unmodifiableMap(this.mappedData),
          Iterables.unmodifiableIterable(this.unmappedData), Collections.unmodifiableMap(this.mappedRollbackData),
          Iterables.unmodifiableIterable(this.unmappedRollbackData));
    }
  }
}
