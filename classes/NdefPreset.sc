
NdefPreset : EnvirPreset {

	classvar <all;
	var <key;

	*initClass { all = () }

	*proxyClass { ^Ndef }

	*new { |key, settings|

		var res, proxy;
		if (key.isKindOf(this.proxyClass)) {
			proxy = key;
			key = proxy.key;
		};

		res = all[key];

		if (res.isNil) {
			// find proxy with same name
			proxy = Ndef.dictFor(Server.default)[key];

			if (proxy.notNil) {
				res = super.new(proxy, settings).prAdd(key);
			} {
				"% - no preset or proxy found.\n".postf(this.proxyClass);
			};
		};
		^res
	}

	prAdd { arg argKey;
		key = argKey;
		all.put(argKey, this);
	}

	proxy_ { |px|
		if (px.isKindOf(Pdef)) {
			proxy = px;
			this.useHalo(proxy);
			// properly init state
			this.currFromProxy;
			currSet = targSet = this.getSet(\curr);
			this.setPath;
		};
	}

	// DIFFERENT FROM other ProxyPresets
	getFromProxy { |except| ^proxy.getKeysValues(except) }

	storeArgs { ^[key] }
	printOn { | stream | ^this.storeOn(stream) }

}
