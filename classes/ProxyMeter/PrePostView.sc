
PrePostView {
	classvar <>preCol, <>postCol;
	var <uv, <preAmp = 0, <postAmp = 0;

	*initClass {
		preCol = Color.yellow(1.0, 0.4);
		postCol = Color.green(0.7, 0.4);
	}

	*new { |parent, bounds|
		^super.new.init(parent, bounds);
	}

	*forMonitor { |monitorGui|
		var sliderZone = monitorGui.zone.children[0];
		var slider = sliderZone.children[1];

		^this.new(sliderZone, slider.bounds);
	}

	init { |parent, bounds|
		uv = UserView(parent, bounds);
		uv.background_(Color(1,1,1,0));
		uv.acceptsMouse_(false);

		// horizontal for now:
		uv.drawFunc = { |uv|
			var bounds = uv.bounds;
			var maxwid = bounds.width - 4;
			var height =bounds.height - 4;

			Pen.color_(preCol);
			Pen.addRect(Rect(2,2,preAmp * maxwid, height));
			Pen.fill;

			Pen.color_(postCol);
			Pen.addRect(Rect(2,2, postAmp * maxwid, height));
			Pen.fill;
		};
	}

	setAmps { |pre = 0, post = 0|
		preAmp = pre.sqrt; postAmp = post.sqrt;
		uv.refresh;
	}

	remove { this.setAmps(0,0) }
}
