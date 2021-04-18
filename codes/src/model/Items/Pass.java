package model.Items;


import model.Utils.Pos2D;

import java.io.Serializable;

public class Pass extends Item implements UsableOn, Serializable {

	private final PassType PASSTYPE;

	public Pass(String tag, String description, PassType p) {
		super(tag, description, new Pos2D(0, 0), true, false);
		this.PASSTYPE = p;
	}

	public Pass(String tag, String description, PassType p, Pos2D pos2D) {
		super(tag, description, pos2D, true, false);
		this.PASSTYPE = p;
	}

    public PassType getPassType() {
		return this.PASSTYPE;
	}

	@Override
	public void isUsed(UsableBy u) {
		System.out.println("Your pass must be used on something !");
	}
}

