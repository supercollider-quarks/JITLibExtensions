// proxy has an envir for parameters
// typically numerical, may also be different.
// if different, special handling is needed.

ProxyPreset {

	classvar <>settingsComment = "/*
///// These are the settings for %:
///// you can edit the textfile, e.g. choosing better preset names,
///// tuning parameter values, or deleting unwanted presets.
///// When done, save the textfile where it is, and
///// then load the edited settings again with:
%.loadSettings(clear: true);
*/\n\n";

	var <proxy, <namesToStore, <settings, <specs, <>morphFuncs;
	var <currSet, <targSet, <>addsToTop = false, <>morphVal = 0, <morphTask;

	var <>storeToDisk = false, <>storePath;

	*initClass {

	}

	*new { |proxy, namesToStore, settings, specs, morphFuncs|

		if (proxy.isNil) {
			warn("EnvirPreset cannot be empty!");
			^nil
		};

		^super.newCopyArgs(proxy, namesToStore,
			settings, specs, morphFuncs).init;
	}

	init {
		this.useHalo;
		this.initTask;
		this.checkSpecsMissing;
		// init morph here already?
		this.currFromProxy;
		this.setCurr(\curr);
		this.setTarg(\curr);
	}

	checkName { |name|
		^(name ?? { "set_" ++ (settings.size + 1) }).asSymbol
	}

	checkSpecsMissing { |autoFill = false, dialog = false|
		// for all missing specs, ask user ...

		var missingSpecNames = namesToStore.select {|name|
			var spec;
			proxy.getSpec(name).isNil;
		};

		if (missingSpecNames.notEmpty) {
			"// % for % is missing specs for % parameters!\n"
			"// Please supply them:\n".postf(this.class, proxy, missingSpecNames.size);
			missingSpecNames.do { |specName|
				"%.addSpec(%, [_min_,_max_,_warp_,_step_,_defaultval_]);\n".postf(proxy, specName.cs);
			};
			if (dialog) { this.specsDialog(missingSpecNames) };
		};
	}

	// sync with object halo as well as possible
	useHalo {

		var haloNames = proxy.getHalo(\namesToStore);
		var haloSpecs = proxy.addSpec.getSpec; // init them

		if (namesToStore.isNil) {
			namesToStore = haloNames ?? { proxy.controlKeys.asArray.sort };
			proxy.addHalo(\namesToStore, namesToStore);
		} {
			if (haloNames.notNil) {
				warn("%: namesToStore given: ".format(this)
					+ namesToStore.asCompileString +
					"\n  and in proxy halo: "
					+ haloNames.asCompileString ++ "!"
					"\n  using my namesToStore.");
			} {
				// sync names back to proxy
				proxy.addHalo(\namesToStore, namesToStore);
			};
		};

		if (specs.isNil) {
			specs = ();
		} {
			if (haloSpecs.notEmpty) {
				warn("ProxyPreset: specs given: "
					+ specs.asCompileString +
					"\n  and in proxy halo: "
					+ haloSpecs.asCompileString ++ "!"
					"\n  using my specs.parent_(haloSpecs).");
			} {
				proxy.addHalo(\spec, haloSpecs);
			};
		};
		specs.parent = haloSpecs;

		// settings and morphFuncs belong to preset:
		settings = settings ?? { List[] };
	}

	initTask {

		morphTask = TaskProxy({ |ev|
			var numSteps, baseStep, morphState;
			ev[\dt] = ev[\dt] ? 0.01;
			ev[\morphTime] = ev[\morphTime] ? 1;

			numSteps = (ev[\morphTime] / ev[\dt]).round(1).max(1);
			baseStep = 1 / numSteps;
			morphState = 1.0;

			numSteps.do { |i|
				this.morphTo( baseStep / morphState.max(baseStep));
				morphState = (morphState - baseStep).clip(0.0, 1.0);
				ev[\dt].wait;
			};
			ev[\doneFunc].value;
		});
	}

	addSet { |name, values, toDisk=false, toTop|
		var index;
		name = name ?? Date.getDate.stamp;
		name = this.checkName(name);
		index = this.getIndex(name);

		values = values ?? { this.getFromProxy.copy };

		// write settings before storage to backup
		if (storeToDisk or: toDisk) {
			this.writeSettings(storePath.splitext.insert(1, "_BK.").join, true);
		};

		if (index.notNil) {
			settings.put(index, name -> values)
		} {
			if (toTop ? addsToTop) {
				settings.insert(1, name -> values)
			} {
				settings.add(name -> values)
			};
		};
		// friendlier with auto-backup...
		if (toDisk) { this.writeSettings(overwrite: true); };
	}

	removeSet { |name, toDisk=false|
		var index = this.getIndex(name);
		if (index.notNil, {
			settings.removeAt(index);
			if (storeToDisk or: toDisk) {
				this.writeSettings(overwrite: true)
			}
		});
	}

	addSettings { |list|
		list.do { |assoc| this.addSet(assoc.key, assoc.value) };
	}
	removeSettings { |names| names.do(this.removeSet(_)) }

	getSetNames { ^settings.collect(_.key) }

	getIndex { |name| ^settings.detectIndex({ |assoc| assoc.key == name }) }

	currIndex { ^settings.indexOf(currSet) }
	targIndex { ^settings.indexOf(targSet) }

	getSet { |name|
		var index = this.getIndex(name);
		^if (index.notNil) { settings[index] } { nil };
	}

	getSetNorm { |name|
		^this.getSet(name).value
		.collect { |pair|
			var name, val; #name, val = pair;
			[name, proxy.getSpec(name).unmap(val)];
		}
	}

	setRelFrom { |name, values|
		var newSettings = this.getSetNorm(name) + values;
		proxy.setUni(*newSettings.flatten(1));
	}

	// if index within settings range, take that preset,
	// else make a new randSet with index as seed
	setCurrIndex { |index, rand = 1.0|
		var found = settings[index];
		if (found.notNil) {
			this.setCurr(found.key)
			^found.key
		} {
			this.setRand(rand, seed: index);
			^("rd." ++ index)
		}
	}

	setCurr { |name|
		var foundSet = this.getSet(name);
		if (foundSet.notNil) {
			currSet = foundSet;
		};
	}

	setTarg { |name, setCurr=true|
		var foundSet;
		foundSet = this.getSet(name);
		if (foundSet.notNil) { targSet = foundSet; };
		if (setCurr) { this.prepMorph; };
	}

	currFromProxy {
		this.addSet(\curr, this.getFromProxy);
	}

	// assume proxy has an environment
	// to do - this could also be supported
	// with proxy.getKeysValues, as with NodeProxies.
	getFromProxy { |except|
		var envir = proxy.envir;
		var res = [];
		if (envir.isNil) { ^[] };
		namesToStore.copy.removeAll(except).collect { |name|
			var val = envir[name];
			res = res.add([name, envir.at(name)]);
		};
		^res
	}

	// speedup variants
	getCurr {
		^namesToStore.collect { |name| proxy.get(name) }
	}

	getCurrUni {
		^namesToStore.collect { |name| proxy.getUni(name) }
	}

	stepCurr { |incr=1|
		var currIndex = settings.indexOf(currSet) ? 0;
		this.setCurr(settings.wrapAt(currIndex + incr).key);
	}

	stepTarg { |incr=1|
		var targIndex = settings.indexOf(targSet) ? 0;
		this.setTarg(settings.wrapAt(targIndex + incr).key);
	}

	setProxy { |name| proxy.set(*this.getSet(name).value.flatten(1)) }


	// STORAGE to Disk:
	// keep them next to the text file they are created with;
	// or maybe make a preset/settings folder?

	setPath { |name|
		this.storePath_(this.presetPath(name)); // make it once
	}

	presetPath { |name|
		^(thisProcess.nowExecutingPath ? "").dirname
		+/+ (name ?? { this.proxy.key ++ ".pxpreset.scd" });
	}

	loadSettings { |path, clear = false|
		path = path ?? { this.storePath ?? { this.setPath; this.storePath } };
		if (clear) { settings.clear };
		this.addSettings(path.load);
	}

	settingsString {
		var comment = settingsComment.format(this, this);
		var setStr = settings.asCompileString
		.replace("List[ (", "List[\n\t(")
		.replace("), (", "), \n\t(")
		.replace("]) ]", "]) \n]\n");
		^comment ++ setStr ++ "\n"
	}

	writeSettings { |path, overwrite=false|
		var file;
		path = path ?? { this.storePath };
		// check first and copy as backup ...
		if (overwrite.not) {
			if (File.exists(path)) {
				warn("ProxyPreset: file" + path + " exists!");
				^this;
			}
		};

		file = File(path, "w");
		if (file.isOpen.not) {
			"% cannot open file at path %.\n".postf(this, path);
			^this
		};

		file.write(this.settingsString);
		file.close;
	}

	// randomize settings:

	randSet { |rand=0.25, startSet, except, seed|

		var randKeysVals, set, randRange, oldRandData;
		var mappings;
		// vary any given set too?
		set = this.getSet(startSet).value ?? {
			this.getFromProxy(except);
		};

		if (except.notNil) {
			set = set.reject { |pair| except.includes(pair[0]); };
		};

		mappings = proxy.nodeMap.mappingKeys;
		if (mappings.notNil) {
			set = set.reject { |pair| mappings.includes(pair[0]) };
		};

		{
			randKeysVals = set.collect { |pair|
				var key, val, normVal, randVal, spec;
				#key, val = pair;
				spec = proxy.getSpec(key);
				if (spec.notNil) {
					normVal =  spec.unmap(val);
					randVal = rrand(
						(normVal - rand).max(0),
						(normVal + rand).min(1)
					);
					[key, spec.map(randVal)];
				} {
					"no spec: %\n".postf([key, val]);
					[]
				};
			};
		}.valueSeed(seed);

		^randKeysVals;
	}


	someRand { |rand=0.1, ratio = 0.5, seed|

		var namesToDrop, oldRandData;
		var keys = namesToStore;
		var numToKeep = (keys.size * ratio).clip(1, keys.size).round(1).asInteger;

		{
			namesToDrop = keys.scramble.drop(keys.size - numToKeep);
			this.setRand(rand, except: namesToDrop, seed: seed);

		}.valueSeed(seed);

	}

	setRand { |rand, startSet, except, seed|
		rand = rand ?? { exprand(0.001, 0.25) };
		proxy.set(*this.randSet(rand, startSet, except, seed).flatten(1));
		this.prepMorph;
	}

	// morphing:
	blendSets { |blend = 0.5, set1, set2|
		^set1.blend(set2, blend);
	}

	prepMorph {
		this.currFromProxy;
		this.setCurr(\curr);
		this.morphVal_(0);
	}

	morph { |blend, name1, name2, mapped=true|
		morphVal = blend;
		proxy.set(*(this.blend(blend, name1, name2, mapped).flatten(1)));
	}

	morphTo { |blend, name, mapped=true|
		this.currFromProxy;
		name = name ? targSet.key;
		proxy.set(*(this.blend(blend, \curr, name, mapped).flatten(1)));
	}

	// from a current morphVal to a different one
	morphValStep { |inMorphVal|
		var newMorphVal = inMorphVal.clip(0.0, 1.0);
		var oldMorphVal = morphVal;
		var morphStep = newMorphVal - oldMorphVal;
		var morphTarg, blendVal, newSet;

		if (morphStep == 0) { ^this };

		if (morphStep > 0) {
			if (currSet.isNil) { ^this };
			morphTarg = targSet.key;
			blendVal = morphStep / (1 - oldMorphVal).max(morphStep);
		} {
			if (targSet.isNil) { ^this };
			morphStep = morphStep.abs;
			morphTarg = currSet.key;
			blendVal = morphStep / oldMorphVal.max(morphStep);
		};
		// "blendVal: % target: %\n".postf(blendVal, morphTarg);

		this.morphTo(blendVal, morphTarg);
		morphVal = newMorphVal;
	}

	xfadeTo { |target, dur, doneFunc|
		var newTargSet;
		if (target.notNil) {
			newTargSet = this.getSet(target);
			if (newTargSet.notNil) {
				targSet = newTargSet;
			} {
				"ProxyPreset: target setting % not found - not xfading.".postf(target);
			};
			morphTask.set(\morphTime, dur);
			morphTask.set(\doneFunc, doneFunc);
			morphTask.stop.play;
		};
	}

	blend { |blend = 0.5, name1, name2, mapped=true|
		var set1, set2;
		set1 = if (name1.isNil, currSet, { this.getSet(name1) }).value;
		set2 = if (name2.isNil, targSet, { this.getSet(name2) }).value;

		if (blend == 0) { ^set1 };
		if (blend == 1) { ^set2 };

		if (set1.isNil) {
			"cannot blend: set % is missing.\n".postf(name1);
			^this;
		};
		if (set2.isNil) {
			"cannot blend: set % is missing.\n".postf(name2);
			^this;
		};

		if (mapped) {
			set1 = this.unmapSet(set1);
			set2 = this.unmapSet(set2);
			^this.mapSet(this.blendSets(blend, set1, set2))
		} {
			^this.blendSets(blend, set1, set2)
		}
	}

	// expects just list of [key, val]s
	mapSet { |set|
		var key, val;
		^set.collect { |pair|
			#key, val = pair;
			[key, specs[key].map(val)]
		}
	}
	// expects just list of [key, val]s
	unmapSet { |set|
		var key, val;
		^set.collect { |pair|
			var spec;
			#key, val = pair;
			spec = specs[key];
			if (spec.notNil) {
				[key, spec.unmap(val)]
			} {
				"%: no spec for %!\n".postf(thisMethod, key);
				[]
			};
		}
	}
}
