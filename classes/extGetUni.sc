// for all objects that use .set for numerical params,
// and support getSpec (via Halo)
+ Object {

	setUni { |...args| this.set(*this.mapPairs(args)); }
	getUni { |... keys|
		keys.collect { |key|
			this.unmapPairs(key, this.get(key));
		}
	}

	setBi { |...args| this.set(*this.mapPairs(args, bipolar: true)); }
	getBi { |... keys|
		keys.collect { |key|
			this.unmapPairs(key, this.get(key), bipolar: true);
		}
	}

	mapPairs { |pairs, bipolar = false|
		var mappedPairs = [];
		pairs.pairsDo { |param, val, i|
			var spec = this.getSpec(param);
			if (spec.notNil) {
				if(bipolar) { val = val.biuni };
				mappedPairs = mappedPairs ++ [param, spec.map(val)];
			} {
				("%.%: no spec for %.\n").postf(
					this, thisMethod.name, param.cs);
			};
		};
		^mappedPairs;
	}

	unmapPairs { |pairs, bipolar = false|
		var unmappedPairs = [], temp;
		pairs.pairsDo { |param, val, i|
			var spec = this.getSpec(param);
			if (spec.notNil) {
				temp = spec.map(val);
				if(bipolar) { temp = temp.unibi };
				unmappedPairs = unmappedPairs ++ [param, temp];

			} {
				("%.%: no spec for %.\n").postf(
					this, thisMethod.name, param.cs);
			};
		};
		^unmappedPairs;
	}
}

+ NPVoicer {
	setUniAt { | index ... args |
		proxy.setAt(index, *proxy.mapPairs(args));
 	}
}
