package com.hccampos.smoothscroll;

import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * Main plugin
 */
public class SmoothScrollPlugin extends AbstractProjectComponent {
    private Project _project;
    private Logger _logger = Logger.getInstance(getClass());

    public SmoothScrollPlugin(Project project) {
        super(project);

        _project = project;
    }

    public void initComponent() {
        _project.getMessageBus()
               .connect()
               .subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorListener());

        _logger.debug("SmoothScroll initialized");
    }

    public void disposeComponent() {
    }

    @NotNull
    @Override
    public String getComponentName() {
        return "SmoothScrollProjectComponent";
    }

    @Override
    public void projectOpened() {
    }

    @Override
    public void projectClosed() {
    }
}