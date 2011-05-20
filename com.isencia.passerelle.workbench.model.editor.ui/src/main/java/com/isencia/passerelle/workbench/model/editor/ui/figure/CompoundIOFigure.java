package com.isencia.passerelle.workbench.model.editor.ui.figure;

import org.eclipse.draw2d.Clickable;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;

public abstract class CompoundIOFigure extends ActorFigure {
	@Override
	protected IFigure generateBody(Image image, Clickable[] clickables) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected int getAnchorHeight() {
		// TODO Auto-generated method stub
		return super.getAnchorHeight()/4;
	}




	public final static int DEFAULT_WIDTH = 28;
	public final static int MIN_HEIGHT = 15;

	public CompoundIOFigure(String name, Class type) {
		super(name, type, null, new Clickable[] {});

	}

	protected abstract Color getBackGroundcolor();
	protected int getDefaultWidth() {
		return DEFAULT_WIDTH;
	}
	protected int getMinHeight() {
		return MIN_HEIGHT;
	}
}
