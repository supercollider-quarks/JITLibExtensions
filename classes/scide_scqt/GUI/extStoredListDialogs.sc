+ StoredList {
	*boundsAround { |loc, winSize, flip = true|
		var rect;
		loc = loc ?? { Window.screenBounds.center };
		rect = Rect.aboutPoint(loc, winSize.x * 0.5, winSize.y * 0.5);
		if (flip) { rect = Window.flipY(rect) };
		^rect
	}

	storeDialog { |name, newset, loc| 		// check before overwriting a setting?
		var bounds = StoredList.boundsAround(loc, (200@30));
		var txt = EZText(nil, bounds, "store as:");

		name = name ?? { Array.rand(8, 97, 122).collect(_.asAscii).join };

		txt.textField.string = name;
		txt.textField.action = { |tf|
			"% - adding setting: ".postf(this);
			this.add(tf.string.asSymbol.postcs, newset);
			txt.textField.parent.parent.close;
		};
		^txt
	}

	deleteDialog { |loc|
		var bounds = StoredList.boundsAround(loc, (250@400));
		var win, names, ezlist;

		names = this.names;

		win = Window("delete", bounds).front;
		win.addFlowLayout;
		ezlist = EZListView(win, win.bounds.insetBy(4, 4),
			"DELETE presets from\n%:"
			"\nselect and backspace".format(this),
			names, nil, labelHeight: 50);
		ezlist.labelView.align_(\center);
		ezlist.view.resize_(5);
		ezlist.widget.resize_(5);
		ezlist.widget.keyDownAction_({ |view, char|
			if(char.toLower == $d) {
				"% - removing setting: ".postf(this);
				this.removeAt(view.item.postcs);
				view.items = this.names;
			};
		});
		^win
	}
}