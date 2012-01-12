
PrePostView { 
	var <parent, <preView, <postView, volSlider, <>monitorGui;
	
	*new { |parent, bounds, volSlider| 
		bounds = bounds ?? { parent.bounds };
		^super.newCopyArgs(parent).init( bounds, volSlider);
	}
	
	*forMonitor { |monitorGui| 
		var sliderZone = monitorGui.zone.children[0];
		var slider = sliderZone.children[1];
		
		^this.new(sliderZone, nil, slider).monitorGui_(monitorGui);
	}
		// probably obsolete
	*forNdefGui { |ndefGui|
		^this.forMonitor(ndefGui.monitorGui);
	}	
		// probably obsolete
	*forMixer { |mixer|
		^mixer.arGuis.collect { |ndefgui|
			this.forNdefGui(ndefgui);
		}
	}
	
	init { |bounds, volSlider|
		if (volSlider.notNil) { 
			volSlider.knobColor = Color.black;
			bounds = volSlider.bounds;
		};
		
		preView = RangeSlider(parent, bounds);
		preView.enabled = false;
		preView.knobColor_(Color.green(1.0, 0.3));
		preView.hi_(0.5);
		
		postView = RangeSlider(parent, bounds);
		postView.enabled = false;
		
		postView.knobColor_(Color.green(0.6, 0.4));
		postView.hi_(0.2);
	}
	
	setAmps { |preAmp = 0, postAmp = 0| 
		if (preView.isClosed.not) { 
			// optical scaling for sliders - amp warp.
			preAmp = (preAmp ? 0).sqrt; 
			postAmp = (postAmp ? 0).sqrt; 
			preView.hi_(preAmp).lo_(postAmp);
			postView.hi_(postAmp);
		};
	}
	
	remove { 
		[preView, postView].do { |view| 
			if (view.isClosed.not) { view.remove }; 
		}
	}
}