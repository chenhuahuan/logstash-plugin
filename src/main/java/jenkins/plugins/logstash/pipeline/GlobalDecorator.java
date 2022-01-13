package jenkins.plugins.logstash.pipeline;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import hudson.model.Queue;
import hudson.model.Run;
import jenkins.plugins.logstash.LogstashConfiguration;
import jenkins.plugins.logstash.LogstashOutputStream;
import jenkins.plugins.logstash.LogstashWriter;
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.log.TaskListenerDecorator;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressFBWarnings(value="SE_NO_SERIALVERSIONID")
public class GlobalDecorator extends TaskListenerDecorator {
  private static final Logger LOGGER = Logger.getLogger(GlobalDecorator.class.getName());

  private transient Run<?, ?> run;
  private String stageName;
  private String agentName;
  private boolean isEnableBuildScopedConnection;
  AtomicBoolean isBuildScopedConnectionBroken;

  public GlobalDecorator(WorkflowRun run) {
    this(run, null, null, false);
  }
  public GlobalDecorator(WorkflowRun run, String stageName, String agentName) {
    this(run, null, null, false);
  }

  public GlobalDecorator(WorkflowRun run, String stageName, String agentName, boolean isEnableBuildScopedConnection) {
    LOGGER.log(Level.INFO, "Creating decorator for {0}", run.toString());
    this.run = run;
    this.stageName = stageName;
    this.agentName = agentName;
    this.isBuildScopedConnectionBroken = new AtomicBoolean(false);
    this.isEnableBuildScopedConnection = isEnableBuildScopedConnection;
  }

  @Override
  public OutputStream decorate(OutputStream logger) throws IOException, InterruptedException {
    LogstashWriter writer = new LogstashWriter(run, logger, null, StandardCharsets.UTF_8, stageName, agentName);
    LogstashOutputStream out = new LogstashOutputStream(logger, writer, isBuildScopedConnectionBroken, isEnableBuildScopedConnection, run.toString());
    return out;
  }

  @Extension
  public static final class Factory implements TaskListenerDecorator.Factory {

    @Override
    public TaskListenerDecorator of(FlowExecutionOwner owner) {
      LOGGER.log(Level.INFO, "isEnableGlobally---{0}", LogstashConfiguration.getInstance().isEnableGlobally());
      LOGGER.log(Level.INFO, "isEnableBuildScopedConnection---{0}", LogstashConfiguration.getInstance().isEnableBuildScopedConnection());
      if (!LogstashConfiguration.getInstance().isEnableGlobally()) {
        return null;
      }
      try {
        Queue.Executable executable = owner.getExecutable();
        if (executable instanceof WorkflowRun) {
          LOGGER.log(Level.INFO, "Running in new Global Decorator----");
          LOGGER.log(Level.INFO, "isEnableBuildScopedConnection---{0}", LogstashConfiguration.getInstance().isEnableBuildScopedConnection());
          if (LogstashConfiguration.getInstance().isEnableBuildScopedConnection()) {
            return new GlobalDecorator((WorkflowRun) executable, null,null,true);
          } else {
            return new GlobalDecorator((WorkflowRun) executable);
          }
        }
      } catch (IOException x) {
        LOGGER.log(Level.WARNING, null, x);
      }
      return null;
    }
  }
}
