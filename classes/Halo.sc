
Halo : Library {
	classvar <lib;

		// shorter posting
	nodeType { ^Event }

	*initClass {
		lib = lib ?? { Halo.new };
	}

	*put { |...args|
		lib.put(*args);
		args[0].changed(*args[1..]);
	}

	*at { | ... keys| ^lib.at(*keys); }

	*postTree {
		this.lib.postTree
	}
}

+ Object {

	addHalo { |...args|
		Halo.put(this, *args);
	}

	getHalo { |... keys|
		if (keys.isNil) { ^Halo.at(this) };
		^Halo.at(this, *keys);
	}

	clearHalo { Halo.lib.put(this, nil) }

	adieu {
		this.clear;
		this.releaseDependants;
		this.clearHalo;
	}

	checkSpec {
		var specs = Halo.at(this, \spec);
		var func = { |who, what|
			if (what == \clear) {
				this.removeDependant(func);
				this.clearHalo;  // will drop the func reference too
			};
		};

		if (specs.notNil) { ^specs };

		specs = ();
		if (this.isKindOf(Class)) {
			specs.parent_(Spec.specs);
		} {
			specs.parent_(this.class.checkSpec);
		};

		Halo.put(this, \spec, specs);
		if(Halo.at(this, \clearWatcher).isNil) {
			Halo.put(this, \clearWatcher, func);
			this.addDependant(func);
		};
		^specs
	}

	// the ones for specs will be a common use case,
	// others could be done similarly:
	addSpec { |...pairs|
		this.checkSpec;
		if (pairs.notNil) {
			pairs.pairsDo { |name, spec|
				if (spec.notNil) { spec = spec.asSpec };
				Halo.put(this, \spec, name, spec);
			}
		};
	}

	getSpec { |name|
		var spec;
		var specs = Halo.at(this, \spec);
		if (name.isNil) { ^specs };
		if (specs.notNil) { spec = specs.at(name) };
		if (spec.isNil and: { name == '#' }) {
			name = this.key;
			if (specs.notNil) { spec = specs.at(name) };
		};
		^spec ?? { name.asSpec }
	}

	addTag { |name, weight = 1|
		Halo.put(this, \tag, name, weight);
	}
		// returns tag weight
	getTag { |name|
		if (name.isNil) { ^Halo.at(this, \tag) };
		^Halo.at(this, \tag, name);
	}

	// categories also have weights
	addCat { |name, weight = 1|
		Halo.put(this, \cat, name, weight);
	}
		// returns cat weight
	getCat { |name|
		if (name.isNil) { ^Halo.at(this, \cat) };
		^Halo.at(this, \cat, name);
	}
}
