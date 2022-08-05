/********
///////////////// Possible next extensions ////////////////

*	insert new slotNames by name, or remove existing slotnames, keeping the structure consistent;
	for reconfiguration of the list of proxychain slots that can be used.
	That would require a better gui where the buttons can be updated.

	////////////// not done yet //////////

	// replace a slot given by name
c.replace(\dust, \noyz, mix ->  { |nfreq1=1200| LFDNoise0.ar(nfreq1) });


	insertAt(index, name, funcOrAssoc)
		inserts in the chain at this index,
		replaces if a slot exists there.

c.insertAt(5, \noyz, mix ->  { |nfreq2=1200| GrayNoise.ar(nfreq2) });

	insertAfter(index, name, funcOrAssoc)
	insertBefore(index, name, funcOrAssoc)
		inserts after (or before) a given slot - halfway toward the neighbour.
		e.g.
c.insertAfter(\dust, \klong, \filter -> { |in, freq=400, att=0.01, decay=0.3, slope=0.8|
	Formlet.ar(in, freq * [0.71, 1, 1.4], att, decay * [1/slope, 1, slope]).sum;
});

	// after which slot, name, funcOrAssoc;
c.insertBefore(\dust, \klong, \filter -> { |in, freq=400, att=0.01, decay=0.3, slope=0.8|
	Formlet.ar(in, freq * [0.71, 1, 1.4], att, decay * [1/slope, 1, slope]).sum;
});
******/


ProxyChain {

	classvar <allSources;
	classvar <sourceDicts;
	classvar <all, <>blendSpec;

	var <slotNames, <slotsInUse, <proxy, <sources;

	*initClass {
		allSources = ();
		sourceDicts = ();
		all = ();

		Class.initClassTree(Halo);
		this.addSpec;
		blendSpec = [0, 1].asSpec;
	}

	// old style - not recommended: add list of sources
	*add { |...args|
		args.pairsDo { |srcName, source|
			this.addSource(srcName, source);
		}
	}

	// new style - recommended: add source, level, specs together
	*add3 { |srcName, source, level, specs|
		var dict = this.atSrcDict(srcName);
		this.addSource(srcName, source);
		this.addLevel(srcName, level);
		this.addSpecs(srcName, specs);
		this.checkSourceDictAt(srcName);
	}

	*addSource { |srcName, source|
		var dict = this.atSrcDict(srcName);
		var srcFunc, paramNames, isFilter = false;

		if (source.notNil) {
			// backwards compat - remove!
			allSources.put(srcName, source);

			dict.put(\source, source);
			srcFunc = if (source.isKindOf(Association)) {
				isFilter = [\filter, \filterIn].includes(source.key);
				source.value
			} { source };
			paramNames = srcFunc.argNames.as(Array);
			///// was:
			// paramNames.remove(\in);
			if (isFilter) { paramNames = paramNames.drop(1) };
			dict.put(\paramNames, paramNames);
		}
	}

	*addLevel { |srcName, level|
		var dict = this.atSrcDict(srcName);
		if (level.notNil) { dict.put(\level, level) }
	}

	*addSpecs { |srcName, specs, srcDict|
		var dict = this.atSrcDict(srcName);
		var specDict;

		if (specs.notNil) {
			specDict = dict[\specs] ?? { () };
			dict.put(\specs, specDict);

			specs.keysValuesDo { |parkey, spec|
				var newspec;
				if (spec.isKindOf(Array)) { newspec = spec.asSpec };
				newspec = newspec ?? { this.getSpec(spec) ?? { spec.asSpec } };
				if (newspec.isNil) {
					"%: spec conversion at % - % failed!\n".postf(this, srcName.cs,  parkey.cs)
				} {
					specDict.put(parkey, spec.asSpec);
				}
			}
		}
	}

	*checkDicts {
		sourceDicts.keysDo { |key| this.checkSourceDictAt(key) }
	}

	*checkSourceDictAt { |srcname|
		var dict = sourceDicts[srcname];
		var src = dict[\source];
		var paramNames = dict[\paramNames];

		paramNames.do { |name|
			var spec;
			if (dict[\specs].notNil) { spec = dict[\specs][name] };
			spec = spec ?? { ProxyChain.getSpec(name) };
			spec = spec ?? { name.asSpec };
			if (spec.isNil) {
				"*** ProxyChain: % needs a spec for %!\n".postf(srcname, name);
			}
		}
	}

	*atSrcDict { |key|
		var sourceDict = sourceDicts[key];
		if (sourceDict.isNil) {
			sourceDict = ().parent = this.getSpec;
			sourceDicts.put(key, sourceDict);
		};
		^sourceDict
	}

	*from { arg proxy, slotNames = #[];
		^super.new.init(proxy, slotNames)
	}

	*new { arg key, slotNames, numChannels, server;
		var proxy;
		var res = all.at(key);

		if(res.notNil) {
			if ([slotNames, numChannels, server].any(_.notNil)) {
				"*** %: cannot reset slotNames, numChannels, or server on an existing ProxyChain."
				" Returning % as is.\n".postf(this, res)
			};
			^res
		};

		proxy = NodeProxy.audio(server ? Server.default, numChannels);
		res = this.from(proxy, slotNames);
		if (key.notNil) { all.put(key, res) };

		if(slotNames.notNil) { res.slotNames_(slotNames) };

		^res
	}

	key { ^all.findKeyForValue(this) }
	storeArgs { ^[this.key] }
	printOn { |stream| ^this.storeOn(stream) }

	init { |argProxy, argSlotNames|

		slotNames = Order.new;
		slotsInUse = Order.new;
		sources = ();
		sources.parent_(allSources);

		proxy = argProxy;
		if (proxy.key.notNil) { all.put(proxy.key, this) };

		this.slotNames_(argSlotNames);

		proxy.addSpec;
		proxy.getSpec.parent = this.class.getSpec;
	}

	// TODO: handle case where slots are currently playing!
	// for every slotsInUse, compare old vs new slots:
	// if active slot stays at its index, leave it running
	// if not, remove it at old index
	// and if it is in the new slotNames, add it again at its new index
	slotNames_ { |argSlotNames|
		slotNames.clear;
		argSlotNames.do { |name, i| slotNames.put(i + 1 * 10, name) };
	}

	add { |key, wet, func| 	// assume the index exists
		var index = slotNames.indexOf(key);
			// only overwrite existing keys so far.
		if (func.notNil, { this.sources.put(key, func) });
		this.addSlot(key, index, wet);
	}

	remove { |key|
	 	var oldSlotIndex = slotsInUse.indexOf(key);
		if (oldSlotIndex.notNil) { proxy[oldSlotIndex] = nil; };
		slotsInUse.remove(key);
	}

	addSlot { |key, index, wet|

		var func = sources[key];
		var srcDict = sourceDicts[key];

		var prefix, prevVal, specialKey;
		if (func.isNil) { "ProxyChain: no func called \%.\n".postf(key, index); ^this };
		if (index.isNil) { "ProxyChain: index was nil.".postln; ^this };

		this.remove(key);
		slotsInUse.put(index, key);

		if (func.isKindOf(Association)) {
			prefix = (filter: "wet", mix: "mix", filterIn: "wet")[func.key];
			specialKey = (prefix ++ index).asSymbol;
			prevVal = proxy.nodeMap.get(specialKey).value;
			if (wet.isNil) { wet = prevVal ? 0 };
			// should be handled by
			proxy.addSpec(specialKey, blendSpec);
			proxy.set(specialKey, wet);
		};

		if (srcDict.notNil and: { srcDict.specs.notNil }) {
			srcDict.specs.keysValuesDo { |param, spec| proxy.addSpec(param, spec) };
		};
		proxy[index] = func;
	}

	setSlots { |keys, levels=#[], update=false|
		var keysToRemove, keysToAdd;
		if (update) {
			keysToRemove = slotsInUse.copy;
			keysToAdd = keys;
		} {
			keysToRemove = slotsInUse.difference(keys);
			keysToAdd = keys.difference(slotsInUse);
		};

		keysToRemove.do(this.remove(_));
		keysToAdd.do { |key, i| this.add(key, levels[i]) };
	}

		// forward basic messages to the proxy
	play { arg out, numChannels, group, multi=false, vol, fadeTime, addAction;
		proxy.play(out, numChannels, group, multi=false, vol, fadeTime, addAction)
	}

	playN { arg outs, amps, ins, vol, fadeTime, group, addAction;
		proxy.playN(outs, amps, ins, vol, fadeTime, group, addAction);
	}

	stop { arg fadeTime, reset=false;
		proxy.stop(fadeTime, reset);
	}

	end { arg fadeTime, reset=false;
		proxy.end(fadeTime, reset);
	}

	set { |... args| proxy.set(*args) }

	clear {
		proxy.clear;
		all.removeAt(this.key);
	}

		// JIT gui support
	gui { |numItems = 16, buttonList, parent, bounds, isMaster = false|
		^ProxyChainGui(this, numItems, parent, bounds, true, buttonList, isMaster);
	}


	// introspection & preset support:
	activeSlotNames { ^slotsInUse.array }

	slotIndexFor { |slotName| ^this.activeSlotNames.indexOf(slotName) }

	orderIndexFor { |slotName|
		var rawIndex = this.activeSlotNames.indexOf(slotName);
		if (rawIndex.isNil) {
			"%: no active slot named %!\n".postf(this, slotName.cs);
			^nil
		};
		^slotsInUse.indices[rawIndex];
	}

	keysAt { |slotName|
		var orderIndex = this.orderIndexFor(slotName);
		var obj, names;
		if (orderIndex.isNil) { ^nil };

		obj = proxy.objects[orderIndex];
		names = obj.controlNames.collect(_.name);
		names.removeAll(proxy.internalKeys);
		^names
	}

	keysValuesAt { |slotName|
		var keys = this.keysAt(slotName);
		if (keys.isNil) { ^nil };
		^proxy.getKeysValues(keys);
	}

	getCurr { |except|
		var slotsToGet = this.activeSlotNames.copy;
		slotsToGet.removeAll(except);
		^slotsToGet.collect { |slotName|
			slotName -> this.keysValuesAt(slotName);
		}
	}
}
