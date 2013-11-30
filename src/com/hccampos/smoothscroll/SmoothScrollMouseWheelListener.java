package com.hccampos.smoothscroll;

import com.intellij.openapi.editor.ScrollingModel;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.TextEditor;

import javax.swing.*;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

class SmoothScrollMouseWheelListener implements MouseWheelListener {
    private static final double STEP = 6;
    private static final double MAX_SPEED = 100;
    private static final double MAX_FORCE = 30;
    private static final double FORCE_REDUCTION_FACTOR = 0.2;
    private static final double PSEUDO_FRICTION = 0.95;
    private static final int FPS = 60;
    private static final int MILLIS_PER_FRAME = 1000 / FPS;

    private ScrollingModel _scrollingModel;
    private double _vel = 0.0;
    private double _force = 0.0;
    private boolean _animating = false;

    /**
     * Constructor for our MouseWheelListener.
     *
     * @param editor
     *      The file editor to which smooth scrolling is to be added.
     */
    public SmoothScrollMouseWheelListener(FileEditor editor) {
        _scrollingModel = ((TextEditor) editor).getEditor().getScrollingModel();
        _scrollingModel.disableAnimation();
    }

    /**
     * Starts animating the scroll offset.
     */
    public void startAnimating() {
        if (_animating) { return; }

        Thread _animationThread = new Thread() {
            public void run() {
                while (_animating) {
                    update();
                    try {
                        Thread.sleep(MILLIS_PER_FRAME);
                    } catch (InterruptedException e) {
                        System.out.println(e.getMessage());
                    }
                }
            }
        };

        _animationThread.start();
        _animating = true;
    }

    /**
     * Stops animating the scroll offset.
     */
    public void stopAnimating() {
        _animating = false;
    }

    /**
     * Updates the velocity, force and acceleration acting on the scroll offset and then updates
     * the scroll offset according to these parameters.
     */
    protected void update() {
        _force *= FORCE_REDUCTION_FACTOR;
        _vel += _force;  // Note that because m=1 we have: F=ma <=> F=a
        _vel *= PSEUDO_FRICTION;
        _vel = limitMagnitude(_vel, MAX_SPEED);

        double deltaPos = _vel;

        if (Math.abs(deltaPos) >= 1.0f) {
            SwingUtilities.invokeLater(new UpdateScrollRunnable((int) deltaPos));
        }
    }

    /**
     * Function which is called when the mouse wheel is rotated.
     *
     * @param e
     *      The object which contains the information of the event.
     */
    public void mouseWheelMoved(MouseWheelEvent e) {
        double wheelDelta = e.getPreciseWheelRotation();

        _force += wheelDelta * STEP;

        double forceMagnitude = Math.abs(_force);
        double normalizedForce = (_force / forceMagnitude);
        _force += normalizedForce * (Math.pow(2, Math.min(50, forceMagnitude / 2)) - 1);

        // Make sure the force does not exceed MAX_FORCE.
        _force = limitMagnitude(_force, MAX_FORCE);
    }

    /**
     * Makes sure the magnitude of the specified value does not exceed maxMagnitude. If the value
     * exceeds maxMagnitude, it is normalized and the then multiplied by maxMagnitude before being
     * returned.
     *
     * @param value
     *      The value which is to be processed.
     * @param maxMagnitude
     *      The maximum magnitude that the value should have.
     *
     * @return
     *      The specified value or, if the value exceeds maxMagnitude, a new value with the same
     *      signal but with magnitude of maxMagnitude.
     */
    private double limitMagnitude(double value, double maxMagnitude) {
        double magnitude = Math.abs(value);
        if (magnitude > maxMagnitude) {
            return value / magnitude * maxMagnitude;
        } else {
            return value;
        }
    }

    /**
     * Simple Runnable which updates the scroll offset of the editor. We have to do this here
     * because the scroll offset can only be read and set from the event dispatching thread.
     */
    private class UpdateScrollRunnable implements Runnable {
        private int _delta;

        public UpdateScrollRunnable(int delta) {
            _delta = delta;
        }

        public void run() {
            int currentOffset = _scrollingModel.getVerticalScrollOffset();
            _scrollingModel.scrollVertically(Math.max(0, currentOffset + _delta));
        }
    }
}
