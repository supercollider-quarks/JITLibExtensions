+ NodeProxy {

	mapPairs { |pairs, bipolar = false|
		var mappedPairs = [];
		pairs.pairsDo { |param, val, i|
			var spec = this.getSpec(param);
			if (spec.notNil) {
				if(bipolar) { val = val.biuni };
				mappedPairs = mappedPairs ++ [param, spec.map(val)];
			} {
				(this.asString + thisMethod.asString + ":\n"
					+ "no spec found for %.\n").postf(\key);
			};
		};
		^mappedPairs;
	}

	// getu {
	//
	// }

	setUni { |...args| this.set(*this.mapPairs(args)); }
	setBi { |...args| this.set(*this.mapPairs(args, bipolar: true)); }

	setu { | ... args | this.set(*this.mapPairs(args)); }
}

+ PatternProxy {

	mapPairs { |pairs, bipolar = false|
		var mappedPairs = [];
		pairs.pairsDo { |param, val, i|
			var spec = this.getSpec(param);
			if (spec.notNil) {
				if(bipolar) { val = val.biuni };
				mappedPairs = mappedPairs ++ [param, spec.map(val)];
			} {
				(this.asString + thisMethod.asString + ":\n"
					+ "no spec found for %.\n").postf(\key);
			};
		};
		^mappedPairs;
	}

	setUni { |...args| this.set(*this.mapPairs(args)); }
	setBi { |...args| this.set(*this.mapPairs(args, bipolar: true)); }

	setu { | ... args | this.set(*this.mapPairs(args)); }
}
