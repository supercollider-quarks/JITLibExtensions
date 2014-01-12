+ NodeProxy {

	mapPairs { |pairs|
		var mappedPairs = [];
		pairs.pairsDo { |param, val, i|
			var spec = this.getSpec(param);
			if (spec.notNil) {
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

	setu { | ... args |
		this.set(*this.mapPairs(args));
	}
}

+ PatternProxy {

	mapPairs { |pairs|
		var mappedPairs = [];
		pairs.pairsDo { |param, val, i|
			var spec = this.getSpec(param);
			if (spec.notNil) {
				mappedPairs = mappedPairs ++ [param, spec.map(val)];
			} {
				(this.asString + thisMethod.asString + ":\n"
					+ "no spec found for %.\n").postf(\key);
			};
		};
		^mappedPairs;
	}

	setu { | ... args |
		this.set(*this.mapPairs(args));
	}
}
