/* overwrite getSpec

*/

+ NodeProxy {
	getSpec { |key, value|
		var foundspec;

		if (this.respondsTo(\findFirstSpecFor)) {
			foundspec = this.findFirstSpecFor(key);
			if (foundspec.notNil) { ^foundspec }
		};

		^super.getSpec(key, value)
	}
}

