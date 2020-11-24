package org.jzy3d.plot3d.rendering.view;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.jzy3d.chart.Chart;
import org.jzy3d.chart.ChartView;
import org.jzy3d.chart.factories.IChartFactory;
import org.jzy3d.colors.Color;
import org.jzy3d.maths.BoundingBox3d;
import org.jzy3d.maths.Coord3d;
import org.jzy3d.painters.NativeDesktopPainter;
import org.jzy3d.painters.Painter;
import org.jzy3d.plot3d.primitives.Parallelepiped;
import org.jzy3d.plot3d.primitives.PolygonFill;
import org.jzy3d.plot3d.primitives.PolygonMode;
import org.jzy3d.plot3d.primitives.axes.AxisBox;
import org.jzy3d.plot3d.primitives.axes.IAxis;
import org.jzy3d.plot3d.rendering.canvas.ICanvas;
import org.jzy3d.plot3d.rendering.canvas.INativeCanvas;
import org.jzy3d.plot3d.rendering.canvas.IScreenCanvas;
import org.jzy3d.plot3d.rendering.canvas.Quality;
import org.jzy3d.plot3d.rendering.scene.Scene;
import org.jzy3d.plot3d.rendering.tooltips.ITooltipRenderer;
import org.jzy3d.plot3d.rendering.tooltips.Tooltip;
import org.jzy3d.plot3d.rendering.view.modes.ViewPositionMode;

import com.jogamp.opengl.util.awt.Overlay;

/**
 * This view is named AWTView because it uses AWT to handle background and overlay rendering.
 * 
 * 
 * @author Martin Pernollet
 *
 */
public class AWTView extends ChartView {
    
    public AWTView(IChartFactory factory, Scene scene, ICanvas canvas, Quality quality) {
        super(factory, scene, canvas, quality);
    }
    
    public void initInstance(IChartFactory factory, Scene scene, ICanvas canvas, Quality quality) {
		super.initInstance(factory, scene, canvas, quality);
        this.backgroundViewport = new AWTImageViewport();
        this.renderers = new ArrayList<Renderer2d>(1);
        this.tooltips = new ArrayList<ITooltipRenderer>();
	}


    @Override
    public void dispose() {
        super.dispose();
        renderers.clear();
    }
    
    public void project() {
    	((NativeDesktopPainter)painter).getCurrentGL(canvas);
    	
        scene.getGraph().project(painter, cam);
        
        ((NativeDesktopPainter)painter).getCurrentContext(canvas).release();
    }

    /** Perform the 3d projection of a 2d coordinate.*/
    public Coord3d projectMouse(int x, int y) {
    	((NativeDesktopPainter)painter).getCurrentGL(canvas);
    	
    	Coord3d p = cam.screenToModel(painter, new Coord3d(x, y, 0));
        
        ((NativeDesktopPainter)painter).getCurrentContext(canvas).release();
        return p;
    }

    @Override
    protected void renderAxeBox(IAxis axe, Scene scene, Camera camera, Coord3d scaling, boolean axeBoxDisplayed) {
        if (axeBoxDisplayed) {
        	painter.glMatrixMode_ModelView();
            
            scene.getLightSet().disable(painter);

            axe.setScale(scaling);
            axe.draw(painter);
            if (DISPLAY_AXE_WHOLE_BOUNDS) { // for debug
                AxisBox abox = (AxisBox) axe;
                BoundingBox3d box = abox.getWholeBounds();
                Parallelepiped p = new Parallelepiped(box);
                p.setFaceDisplayed(false);
                p.setWireframeColor(Color.MAGENTA);
                p.setWireframeDisplayed(true);
                p.draw(painter);
            }

            scene.getLightSet().enableLightIfThereAreLights(painter);
        }
    }

    @Override
    protected void correctCameraPositionForIncludingTextLabels(Painter painter, ViewportConfiguration viewport) {
    	cam.setViewPort(viewport);
        cam.shoot(painter, cameraMode);
        axis.draw(painter);
        clear();

        //AxeBox abox = (AxeBox) axe;
        BoundingBox3d newBounds = axis.getWholeBounds().scale(scaling);

        if (viewmode == ViewPositionMode.TOP) {
            float radius = Math.max(newBounds.getXmax() - newBounds.getXmin(), newBounds.getYmax() - newBounds.getYmin()) / 2;
            radius += (radius * STRETCH_RATIO);
            cam.setRenderingSphereRadius(radius);
        } else
            cam.setRenderingSphereRadius((float) newBounds.getRadius());

        Coord3d target = newBounds.getCenter();
        Coord3d eye = viewpoint.cartesian().add(target);
        cam.setTarget(target);
        cam.setEye(eye);
    }

    /**
     * Renders all provided {@link Tooltip}s and {@link Renderer2d}s on top of
     * the scene.
     * 
     * Due to the behaviour of the {@link Overlay} implementation, Java2d
     * geometries must be drawn relative to the {@link Chart}'s
     * {@link IScreenCanvas}, BUT will then be stretched to fit in the
     * {@link Camera}'s viewport. This bug is very important to consider, since
     * the Camera's viewport may not occupy the full {@link IScreenCanvas}.
     * Indeed, when View is not maximized (like the default behaviour), the
     * viewport remains square and centered in the canvas, meaning the Overlay
     * won't cover the full canvas area.
     * 
     * In other words, the following piece of code draws a border around the
     * {@link View}, and not around the complete chart canvas, although queried
     * to occupy chart canvas dimensions:
     * 
     * g2d.drawRect(1, 1, chart.getCanvas().getRendererWidth()-2,
     * chart.getCanvas().getRendererHeight()-2);
     * 
     * {@link renderOverlay()} must be called while the OpenGL2 context for the
     * drawable is current, and after the OpenGL2 scene has been rendered.
     */
    @Override
    public void renderOverlay(ViewportConfiguration viewport) {
        if (!hasOverlayStuffs())
            return;
        
        INativeCanvas nCanvas = (INativeCanvas)canvas;

        if (overlay == null)
            this.overlay = new Overlay(nCanvas.getDrawable());


        // TODO: don't know why needed to allow working with Overlay!!!????
        
        painter.glPolygonMode(PolygonMode.FRONT_AND_BACK, PolygonFill.FILL);
        
        painter.glViewport(viewport.x, viewport.y, viewport.width, viewport.height);

        if (overlay != null && viewport.width > 0 && viewport.height > 0) {
            
            try {
                if(nCanvas.getDrawable().getSurfaceWidth()>0 && nCanvas.getDrawable().getSurfaceHeight()>0){
                    Graphics2D g2d = overlay.createGraphics();

                    g2d.setBackground(bgOverlay);
                    g2d.clearRect(0, 0, canvas.getRendererWidth(), canvas.getRendererHeight());

                    // Tooltips
                    for (ITooltipRenderer t : tooltips)
                        t.render(g2d);

                    // Renderers
                    for (Renderer2d renderer : renderers)
                        renderer.paint(g2d, canvas.getRendererWidth(), canvas.getRendererHeight());

                    overlay.markDirty(0, 0, canvas.getRendererWidth(), canvas.getRendererHeight());
                    overlay.drawAll();
                    g2d.dispose();
                }
                
                
                

            } catch (Exception e) {
                LOGGER.error(e, e);
            }
        }
    }

    @Override
    public void renderBackground(float left, float right) {
        if (backgroundImage != null) {
            backgroundViewport.setViewPort(canvas.getRendererWidth(), canvas.getRendererHeight(), left, right);
            backgroundViewport.render(painter);
        }
    }

    @Override
    public void renderBackground(ViewportConfiguration viewport) {
        if (backgroundImage != null) {
            backgroundViewport.setViewPort(viewport);
            backgroundViewport.render(painter);
        }
    }

    /** Set a buffered image, or null to desactivate background image */
    public void setBackgroundImage(BufferedImage i) {
        backgroundImage = i;
        backgroundViewport.setImage(backgroundImage, backgroundImage.getWidth(), backgroundImage.getHeight());
        backgroundViewport.setViewportMode(ViewportMode.STRETCH_TO_FILL);
        // when stretched, applyViewport() is cheaper to compute, and this does
        // not change
        // the picture rendering.
        // bgViewport.setScreenGridDisplayed(true);
    }

    public BufferedImage getBackgroundImage() {
        return backgroundImage;
    }

    public void clearTooltips() {
        tooltips.clear();
    }

    public void setTooltip(ITooltipRenderer tooltip) {
        tooltips.clear();
        tooltips.add(tooltip);
    }

    public void addTooltip(ITooltipRenderer tooltip) {
        tooltips.add(tooltip);
    }

    public void setTooltips(List<ITooltipRenderer> tooltip) {
        tooltips.clear();
        tooltips.addAll(tooltip);
    }

    public void addTooltips(List<ITooltipRenderer> tooltip) {
        tooltips.addAll(tooltip);
    }

    public List<ITooltipRenderer> getTooltips() {
        return tooltips;
    }

    public void addRenderer2d(Renderer2d renderer) {
        renderers.add(renderer);
    }

    public void removeRenderer2d(Renderer2d renderer) {
        renderers.remove(renderer);
    }

    protected boolean hasOverlayStuffs() {
        return tooltips.size() > 0 || renderers.size() > 0;
    }

    protected List<ITooltipRenderer> tooltips;
    protected List<Renderer2d> renderers;
    protected java.awt.Color bgOverlay = new java.awt.Color(0, 0, 0, 0);
    protected AWTImageViewport backgroundViewport;
    protected BufferedImage backgroundImage = null;
    protected Overlay overlay;
}
