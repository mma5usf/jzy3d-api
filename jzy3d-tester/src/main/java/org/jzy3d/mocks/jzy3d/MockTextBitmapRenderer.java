package org.jzy3d.mocks.jzy3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jzy3d.colors.Color;
import org.jzy3d.maths.BoundingBox3d;
import org.jzy3d.maths.Coord2d;
import org.jzy3d.maths.Coord3d;
import org.jzy3d.painters.Painter;
import org.jzy3d.plot3d.rendering.view.Camera;
import org.jzy3d.plot3d.text.align.Halign;
import org.jzy3d.plot3d.text.align.Valign;
import org.jzy3d.plot3d.text.renderers.TextBitmapRenderer;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.glu.GLU;

public class MockTextBitmapRenderer extends TextBitmapRenderer{
    List<Map<String,Object>> callArguments = new ArrayList<>();
    
    @Override
    public BoundingBox3d drawText(Painter painter, GL gl, GLU glu, Camera cam, String text, Coord3d position, Halign halign, Valign valign, Color color, Coord2d screenOffset, Coord3d sceneOffset) {
        Map<String,Object> args = new HashMap<String,Object>();
        args.put("text", text);
        args.put("position", position);
        args.put("halign", halign);
        args.put("valign", valign);
        args.put("color", color);
        args.put("screenOffset", screenOffset);
        args.put("sceneOffset", sceneOffset);

        
        callArguments.add(args);
        
        return new BoundingBox3d();
    }

    public List<Map<String, Object>> getCallArguments() {
        return callArguments;
    }
    
    public Map<String, Object> getCallArguments_whereTextEquals(String text) {
        for(Map<String, Object> args: callArguments){
            if(text.equals(args.get("text"))){
                return args;
            }
        }
        return null;
    }

}
