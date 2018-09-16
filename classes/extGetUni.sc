// for all objects that use .set for numerical params,
// and support getSpec (via Halo)
+ Object {

	setUni { |...args| this.set(*this.mapPairs(args)) }
	setBi { |...args| this.set(*this.mapPairs(args, bipolar: true)) }

	// single, to match proxy.get
	getUni { |name|
		var spec = this.getSpec(name);
		var val = this.get(name);
		if (val.isNil) {
			("%.%: no value for key %.\n").postf(this, thisMethod.name, name.cs);
			^nil
		};
		if (spec.isNil) {
			("%.%: no spec for key %.\n").postf(this, thisMethod.name, name.cs);
			^nil
		};
		^spec.unmap(val);
	}

	// single, to match proxy.get
	getBi { |name|
		var unmappedVal = this.getUni(name);
		^if (unmappedVal.isNil) { nil } { unmappedVal * 2 - 1 };
	}

	getUnis { |...names|
		^names.collect ( this.getUni(_) )
	}
	getBis { |...names|
		^names.collect  ( this.getBi(_) )
	}

	mapPairs { |pairs, bipolar = false|
		var mappedPairs = [];
		pairs.pairsDo { |param, val, i|
			var spec = this.getSpec(param);
			if (spec.notNil) {
				if(bipolar) { val = val + 1 * 0.5 };
				mappedPairs = mappedPairs ++ [param, spec.map(val)];
			} {
				("%.%: no spec for %.\n").postf(
					this, thisMethod.name, param.cs);
			};
		};
		^mappedPairs;
	}
}

+ NPVoicer {
	setUniAt { | index ... args |
		proxy.setAt(index, *proxy.mapPairs(args));
	}
}
