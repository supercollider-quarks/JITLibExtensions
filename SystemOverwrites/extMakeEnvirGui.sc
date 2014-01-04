+ TaskProxy {
	controlKeys {
		^if (envir.notNil) { envir.keys.asArray.sort } { [] };
	}
}

+ TaskProxyGui {

	makeEnvirGui { |lineWidth, height|
		zone.decorator.nextLine.shift(0, 2);

		envirGui = ParamGui(
			try { this.object.envir },
			numItems,
			zone,
			Rect(0, 20, lineWidth, numItems * height),
			false
		);
	}

	object_ { |obj|
		super.object_(obj);
		envirGui.useHalo(this.object);
	}
}

+ NdefParamGui {

	object_ { |obj|
		super.object_(obj);
		this.useHalo(this.object);
	}

	useHalo { |haloObject, myNames = false|
		"NdefParamGui:useHalo gets called.".postln;

		if (haloObject.isNil) {
			warn("NdefParamGui: object is nil, cant use its Halo!");
			^this
		};
		specs = haloObject.getSpec;
		this.updateSliderSpecs;
	}

	getSpec { |key, value|
		var spec = object.getSpec(key) ? Spec.specs[key];
		spec = spec ?? { Spec.guess(key, value) };
		specs.put(key, spec);
		^spec
	}

	updateSliderSpecs {
		widgets.do { |widge, i|
			var editKey = this.editKeys[i];
			if (widge.isKindOf(EZSlider) or: { widge.isKindOf(EZRanger) }) {
				widge.controlSpec = this.getSpec(editKey);
				widge.value_(widge.value);
			}
		};
	}
}
