+ TaskProxy {
	controlKeys {
		var cKeys = this.getHalo(\orderedNames);
		if (cKeys.notNil) { ^cKeys };
		cKeys = if (envir.notNil) { envir.keys.asArray.sort } { [] };
		^cKeys;
	}
}

+ TaskProxyGui {

	object_ { |obj|
		super.object_(obj);
		if(envirGui.notNil) {
			envirGui.useHalo(this.object);
		};
	}

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

}

+ NdefGui {
	object_ { |obj|
		super.object_(obj);
		if(paramGui.notNil) {
			paramGui.useHalo(this.object);
		};
	}
}

+ EnvirGui {
	    // obj is an envir or nil
	    // clear specs when object changes
	object_ { |obj|
		if (this.accepts(obj)) {
			object = obj;
			specs.clear.parent_(nil);
			this.checkUpdate;
		};
	}

	// overwriting global getSpec to get local specs
	// this and useHalo depend on Halo class (in JITLibExtensions)
	getSpec { |key, value|
		var spec =
		object.getHalo(\spec, key)
		      // plug halo of e.g. a tdef into specs.parent
		?? { if (specs.parent.notNil) { specs.parent[key] } }
		?? { specs[key] }
		?? { Spec.specs[key] }
		?? { Spec.guess(key, value) };
		^spec
	}

	useHalo { |haloObject|
		var objSpecs;
		if (haloObject.isNil) { ^this };
		objSpecs = haloObject.getSpec.postln;
		specs.parent_(objSpecs);
		this.updateSliderSpecs;
	}

	updateSliderSpecs { |editKeys|

		if (object.isNil) { specs.clear; ^this };

		editKeys.do { |key, i|
			var currValue = object[key];
			var newSpec = this.getSpec(key, currValue);
			var widge = widgets[i];
			if (newSpec != specs[key]) {
				specs.put(key, newSpec);
				if (widge.isKindOf(EZSlider) or: { widge.isKindOf(EZRanger) }) {
					widge.controlSpec = newSpec;
					widge.value_(currValue);
				};
			};
		}
	}

	// also get specs as state that may have changed
	getState {
		var newKeys, overflow, currSpecs;

		if (object.isNil) { ^(editKeys: [], overflow: 0, keysRotation: 0) };

		newKeys = object.keys.asArray.sort;
		overflow = (newKeys.size - numItems).max(0);
		keysRotation = keysRotation.clip(0, overflow);
		newKeys = newKeys.drop(keysRotation).keep(numItems);
		currSpecs = newKeys.collect{ |key|
			[key, this.getSpec(key, object[key])] };

		^(  object: object,
			editKeys: newKeys,
			overflow: overflow,
			keysRotation: keysRotation,
			specs: currSpecs
		)
	}

	// also updateSliderSpecs
	checkUpdate { |doFull = false|
		var newState = this.getState;
		var newKeys = newState[\editKeys];

		this.updateButtons;

		if (doFull.not and: { newState == prevState }) {
			prevState = newState;
			^this
		};

		if (object.isNil) {
			prevState = newState;
			^this.clearFields(0);
		};

		if (newState[\overflow] > 0) {
			scroller.visible_(true);
			scroller.numItems_(object.size);
			scroller.value_(newState[\keysRotation]);

		} {
			scroller.visible_(false);
		};

		if (newKeys == prevState[\editKeys]) {
			this.setByKeys(newKeys);
		} {
			this.setByKeys(newKeys);
			if (newState[\overflow] == 0) { this.clearFields(newKeys.size) };
		};

		this.updateSliderSpecs(newKeys);

		prevState = newState.put(\object, object.copy);
	}
}

+ NdefParamGui {

	// getState and checkUpdate are copied from 3.7.0
	// for backwards compatibility with pre-3.6.6 versions.
	// delete when obsolete

	getState {
		var settings, newKeys, overflow, currSpecs;

		if (object.isNil) {
			^(name: 'anon', settings: [], editKeys: [], overflow: 0, keysRotation: 0)
		};

		settings = object.getKeysValues;
		newKeys = settings.collect(_[0]);

		overflow = (newKeys.size - numItems).max(0);
		keysRotation = keysRotation.clip(0, overflow);
		newKeys = newKeys.drop(keysRotation).keep(numItems);
		currSpecs = newKeys.collect { |key|
			var pair = settings.detect { |pair| pair[0] == key };
			this.getSpec(key, pair[1]);
		};

		^(object: object, editKeys: newKeys, settings: settings,
			overflow: overflow, keysRotation: keysRotation,
			specs: currSpecs
		)
	}

	checkUpdate {
		var newState = this.getState;

		if (newState == prevState) {
			^this
		};

		if (object.isNil) {
			prevState = newState;
			^this.clearFields(0);
		};

		if (newState[\overflow] > 0) {
			scroller.visible_(true);
			scroller.numItems_(newState[\settings].size);
			scroller.value_(newState[\keysRotation]);

		} {
			scroller.visible_(false);
		};

		if (newState[\editKeys] == prevState[\editKeys]) {
			this.setByKeys(newState[\editKeys], newState[\settings]);
		} {
			this.setByKeys(newState[\editKeys], newState[\settings]);
			if (newState[\overflow] == 0) { this.clearFields(newState[\editKeys].size) };
		};

		this.updateSliderSpecs(newState[\editKeys]);

		prevState = newState;
	}

	updateSliderSpecs { |editKeys|
		var currState;

		if (object.isNil) { specs.clear; ^this };

		currState = object.getKeysValues;

		editKeys.do { |key, i|
			var currValue = currState.detect { |pair| pair[0] == key }[1];
			var newSpec = this.getSpec(key, currValue);
			var widge = widgets[i];
			if (newSpec != specs[key]) {
				specs.put(key, newSpec);
				if (widge.isKindOf(EZSlider) or:
					{ widge.isKindOf(EZRanger) }) {
					widge.controlSpec = newSpec;
					widge.value_(currValue);
				};
			};
		}
	}
}
