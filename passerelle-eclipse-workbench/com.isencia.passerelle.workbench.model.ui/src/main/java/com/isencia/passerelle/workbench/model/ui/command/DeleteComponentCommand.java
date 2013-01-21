package com.isencia.passerelle.workbench.model.ui.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.gef.commands.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ptolemy.actor.Actor;
import ptolemy.actor.TypedIORelation;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.ChangeRequest;
import ptolemy.kernel.util.NamedObj;
import ptolemy.moml.Vertex;

import com.isencia.passerelle.editor.common.model.Link;
import com.isencia.passerelle.editor.common.model.LinkHolder;
import com.isencia.passerelle.editor.common.utils.EditorUtils;
import com.isencia.passerelle.model.Flow;
import com.isencia.passerelle.workbench.model.ui.IPasserelleMultiPageEditor;
import com.isencia.passerelle.workbench.model.ui.utils.EclipseUtils;
import com.isencia.passerelle.workbench.model.utils.ModelChangeRequest;

public class DeleteComponentCommand extends Command implements IRefreshConnections {
  private IPasserelleMultiPageEditor multiPageEditor;
  private LinkHolder linkHolder;

  /**
   * @param linkHolder
   *          the linkHolder to set
   */
  public void setLinkHolder(LinkHolder linkHolder) {
    this.linkHolder = linkHolder;
  }

  public void setMultiPageEditor(IPasserelleMultiPageEditor multiPageEditor) {
    this.multiPageEditor = multiPageEditor;
  }

  private List<Integer> indexList = new ArrayList<Integer>();

  public void addIndex(Integer index) {
    indexList.add(index);
  }

  public void emptyIndexList() {
    indexList.removeAll(indexList);
  }

  private static Logger logger = LoggerFactory.getLogger(DeleteComponentCommand.class);

  private NamedObj container;
  private NamedObj child;

  private CompositeEntity parent;

  private List<DeleteLinkCommand> delecteListCommands = new ArrayList<DeleteLinkCommand>();

  public DeleteComponentCommand() {
    super("Delete");
  }

  public Logger getLogger() {
    return logger;
  }

  public void execute() {
    doExecute();
  }

  protected void doExecute() {

    parent.requestChange(new ModelChangeRequest(this.getClass(), parent, "delete", child) {
      @Override
      protected void _execute() throws Exception {
        Flow toplevel = (Flow) child.toplevel();
        if (child instanceof Vertex) {
          Vertex vertex = (Vertex) child;
          ((TypedIORelation) vertex.getContainer()).setContainer(null);
          container = ((TypedIORelation) vertex.getContainer()).getContainer();
        } else {
          container = child.getContainer();
          EditorUtils.setContainer(child, null);
          for (Integer index : indexList) {
            Comparator comparator = Collections.reverseOrder();
            Collections.sort(indexList, comparator);
            multiPageEditor.removePage(index);
          }
        }
        List<Link> links = new ArrayList<Link>();

        if (child instanceof Actor) {
          Actor actor = (Actor) child;
          List ports = new ArrayList();
          ports.addAll(actor.inputPortList());
          ports.addAll(actor.outputPortList());
          for (Object p : ports) {
            List<Link> portLinks = linkHolder.getLinks(p);
            if (portLinks != null) {
              links.addAll(portLinks);
            }

          }

        } else {
          links = linkHolder.getLinks(child);
        }
        if (links != null && links.size() > 0) {
          delecteListCommands = new ArrayList<DeleteLinkCommand>(links.size());
          for (Link link : links) {
            delecteListCommands.add(new DeleteLinkCommand((CompositeEntity) container, link));
          }
        }
      }
    });

  }

  /*
   * private void reattachToGuides(LogicSubpart part) { if (vGuide != null) vGuide.attachPart(part, vAlign); if (hGuide
   * != null) hGuide.attachPart(part, hAlign); }
   */

  public void redo() {
    doExecute();
  }

  private void restoreConnections(NamedObj child) {

    for (Command cmd : delecteListCommands) {
      cmd.undo();
    }
  }

  public void setChild(NamedObj c) {
    child = c;
  }

  public void setParent(CompositeEntity p) {
    parent = p;
  }

  public void undo() {
    // Perform Change in a ChangeRequest so that all Listeners are notified
    parent.requestChange(new ChangeRequest(child, "undo-delete") {
      @Override
      protected void _execute() throws Exception {
        try {
          if (child instanceof Vertex) {
            Vertex vertex = (Vertex) child;
            ((TypedIORelation) vertex.getContainer()).setContainer((CompositeEntity) container);
          } else {

            EditorUtils.setContainer(child, container);
            restoreConnections(child);
          }
        } catch (Exception e) {
          logger.error("Unable to undo deletion of component", e);

          EclipseUtils.logError(e, "Unable to undo deletion of component", IStatus.ERROR);
        }

      }
    });

  }

}
