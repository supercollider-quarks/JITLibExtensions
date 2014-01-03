/* to do:

NdefPreset:morph assumes both settings have the same order and keys!
this should be checked... find broken presets?



* keep local specs here, in Preset? make missing specs?
* add extras like autorand, autoloop ?
* build into NdefGui as an option, or do NdefPreset(Ndef(\a));
TdefPreset
// keep the presets next to the text file the proxy/preset was created with;
// save settings to disk locally ...
// else make a preset/settings folder in userfolder?
// add timestamp to backup copies of settings?
// use/adapt/generalize for Tdef/Pdef.envir?

* how to morph between lists of symbols ... random choice?
* how to morph between lists of different lengths? (e.g. pattern value lists)?

* check presence of specs for all keys,
* add specsDialog to add / edit current specs.
*** (use Halo approach for keeping specs with proxy? ***
* make a blendSafe method which checks that only values
present in both and settings are blended, and only if
numerical and equal length. otherwise, do probChoice between them based on
blend factor.

*/

// abstract superclass for NdefPreset, TdefPreset, PdefPreset
ProxyPreset {

	var <proxy, <settings, <currSet, <specs, count = 0;
	var <>storeToDisk = true, <>storePath;
	var <targSet, <>morphVal = 0, <morphTask;

	*new { |proxy, settings|
		^super.new.init.proxy_(proxy).addSettings(settings);
	}

	proxy_ { |inProxy|
		// subclasses should overwrite and test for correct class here:
		// if (inProxy.isNil or: { inProxy.isKindOf(this.proxyClass) }) {
			proxy = inProxy;
		    specs = try { proxy.getSpec } ?? { () };
		// };
	}

	init { 		// a list of assocs
		settings =  List[];

		// make specs dict for the proxy if there
		specs = try { proxy.getSpec } ?? { () };

		morphTask = TaskProxy({ |ev|
			var numSteps;
			ev[\dt] = ev[\dt] ? 0.01;
			ev[\morphTime] = ev[\morphTime] ? 1;
			this.prepMorph;

			numSteps = ev[\morphTime] / ev[\dt];
			numSteps.do { |i|
				this.morph(1 + i / numSteps);
				ev[\dt].wait;
			};
		});
	}

	addSet { |name, values, toDisk=false|
		var index;
		if (name.isNil, { count = count + 1; name = "set" ++ count; });
		name = name.asSymbol;
		index = this.getIndex(name);

		// - NDEF-specific! abstract out later //
		values = values ?? { this.getFromProxy.copy };

		// writeBackup
		if (toDisk) {
			this.writeSettings(storePath.splitext.insert(1, "_BK.").join, true);
		};

		if (index.notNil) {
			settings.put(index, name -> values)
		} {
			settings.add(name -> values);
		};
		// friendlier with auto-backup...
		if (toDisk) { this.writeSettings(overwrite: true); };
	}

	removeSet { |name|
		var index = this.getIndex(name);
		if (index.notNil, { settings.removeAt(index) });
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

	setCurr { |name|
		var foundSet = this.getSet(name);
		if (foundSet.notNil) {
			currSet = foundSet;
			proxy.set(*currSet.value);
			this.morphVal_(0);
		};
	}

	setTarg { |name, setCurr=true|
		var foundSet;
		foundSet = this.getSet(name);
		if (foundSet.notNil) { targSet = foundSet; };
		if (setCurr) { this.currFromProxy; };
	}

	currFromProxy {
		this.addSet(\curr, this.getFromProxy);
		this.morphVal_(0)
	}

	// assume proxy has an environment
	// overwrite in specific subclasses!
	getFromProxy { |except|
		var envir = proxy.envir;
		var settings = List[];
		if (envir.notNil) {
			envir.keys.difference(except).asArray.sort.do { |key|
				settings.add([key, envir[key]]);
			};
		};
		^envir
	}

	stepCurr { |incr=1|
		var currIndex = settings.indexOf(currSet);
		this.setCurr(settings.wrapAt(currIndex + incr).key);
	}

	stepTarg { |incr=1|
		var targIndex = settings.indexOf(targSet);
		this.setTarg(settings.wrapAt(targIndex + incr).key);
	}

	setProxy { |name| proxy.set(*this.getSet(name).value) }


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
		path = path ?? { this.storePath };
		if (clear) { settings.clear };
		this.addSettings(path.load);
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
		file.write(this.settings.asCompileString);
		file.close;
	}


	// randomize settings:

	randSet { |rand=0.25, startSet, except|
		var randKeysVals, set, randRange;
		// vary any given set too?
		set = this.getSet(startSet).value ?? { this.getFromProxy(except) };

		randKeysVals = set.collect { |pair|
			var key, val, normVal, randVal, spec;
			#key, val = pair;
			spec = key.asSpec;
			if (spec.notNil, {
				normVal =  spec.unmap(val);
				randVal = rrand(
					(normVal - rand).max(0),
					(normVal + rand).min(1)
				);
				//	[key, val, normVal].postcs;
				[key, spec.map(randVal)]
			});
		};
		^randKeysVals;
	}

	someRand { |rand=0.1, ratio = 0.5|
		var keys = proxy.controlKeys;
		var numToKeep = (keys.size * ratio).clip(1, keys.size).round(1).asInteger;
		var namesToDrop = keys.scramble.drop(keys.size - numToKeep);
		this.setRand(rand, except: namesToDrop);
	}

	setRand { |rand, startSet, except|
		rand = rand ?? { exprand(0.001, 0.25) };
		proxy.set(*this.randSet(rand, startSet, except));
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
		proxy.set(*(this.blend(blend, name1, name2, mapped)));
		morphVal = blend;
	}

	xfadeTo { |target, dur|
		var newTargSet;
		if (target.notNil) {
			newTargSet = this.getSet(target);
			if (newTargSet.notNil) {
				targSet = newTargSet;
			} {
				"ProxyPreset: target setting % not found - not xfading.".postf(target);
			};
			morphTask.set(\morphTime, dur);
			morphTask.stop.play;
		};
	}

	blend { |blend = 0.5, name1, name2, mapped=true|
		var set1, set2;
		set1 = if (name1.isNil, currSet, { this.getSet(name1) }).value;
		set2 = if (name2.isNil, targSet, { this.getSet(name2) }).value;
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
			[key, key.asSpec.map(val)]
		}
	}
	// expects just list of [key, val]s
	unmapSet { |set|
		var key, val;
		^set.collect { |pair|
			#key, val = pair;
			[key, key.asSpec.unmap(val)]
		}
	}

	postSettings {
		("<pxPresetNameHere>.addSettings(" + settings.asCompileString + ")")
		.newTextWindow(proxy.key ++ ".pxpreset.scd");
	}

	storeDialog { |name, loc| 		// check before overwriting a setting?
		var w;
		loc = loc ?? {400@300};
		if (name.isNil, { count = count + 1; name = "set" ++ count; });
		w = Window("", Rect(loc.x, loc.y + 40, 150, 40), false);
		StaticText(w, Rect(0,0,70,20)).align_(\center).string_("name set:");
		TextField(w, Rect(70,0,70,20)).align_(\center)
		.string_(name)
		.action_({ arg field;
			this.addSet(field.value.asSymbol, toDisk: storeToDisk);
			w.close;
		})
		.focus(true);
		w.front;
	}

	deleteDialog { |loc|
		var win, names, ezlist;
		var winOrigin, winSize = (150@200);

		names = this.getSetNames;
		names.remove(\curr);
		loc = loc ?? { (100@400) };
		winOrigin = loc - winSize;

		win = Window("delete", Rect(winOrigin.x, winOrigin.y, 150,200)).front;
		win.addFlowLayout;
		ezlist = EZListView(win, win.bounds.insetBy(4, 4),
			"DELETE presets from\n%:"
			"\nselect and backspace".format(this),
			names, nil, labelHeight: 50);
		ezlist.labelView.align_(\center);
		ezlist.view.resize_(5);
		ezlist.widget.resize_(5);
		ezlist.widget.keyDownAction_({ |view, char|
			if(char == 8.asAscii) {
				this.removeSet(view.items[view.value].postln);
				view.items = this.getSetNames;
			};
		});
		^win
	}

	 specsDialog { |keys, specDict|
		var w, loc, name, proxyKeys, specKeys;
		specDict = specDict ? specs;
		keys = keys ?? [\ida, \jacob, \karlo];
		// { this.currFromProxy.clump(2).flop.first.postln };
		 loc = loc ?? {400@300};
		w = Window("specs please", Rect(loc.x, loc.y + 40, 300, 200)).front;
		w.addFlowLayout;
		StaticText(w, Rect(0,0,290,50)).align_(\center)
		.string_(
			"Please enter specs for the following\nparameter keys:"
			"\n(min, max, warp, step, default, units)"
		);
		keys.collect { |key|
			var eztext;
			eztext = EZText(w, Rect(70,0,290,20), key, { |ez|
				var spec = ez.value.asSpec;
				specDict.put(key, spec);
				[key, spec].postcs;
				},
				[0.0, 1.0, 'lin', 0.0, 0.5]
			);
		};
	}
}

TdefPreset : ProxyPreset {
	classvar <all;
	*initClass { all = () }

	*new { |key, settings|
		var res, proxy;
		if (key.isKindOf(Tdef)) {
			proxy = key;
			key = proxy.key;
		};
		res = all[key];
		if (res.isNil) {
			res = super.new(proxy, settings);
			all.put(key, res);
		};
		^res
	}

	key { ^all.findKeyForValue(this) }

	proxy_ { |px|
		if (px.isKindOf(Tdef)) {
			proxy = px;
		    specs = try { proxy.getSpec } ?? { () };
			// proper init state
			this.currFromProxy;
			currSet = targSet = this.getSet(\curr);
			this.setPath;
		};
	}

	printOn { | stream |
		stream << this.class.name << "(" <<< this.key << ")"
	}
}

PdefPreset : ProxyPreset {
	classvar <all;
	*initClass { all = () }

	*new { |key, settings|
		var res, proxy;
		if (key.isKindOf(Pdef)) {
			proxy = key;
			key = proxy.key;
		};
		res = all[key];
		if (res.isNil) {
			res = super.new(proxy, settings);
			all.put(key, res);
		};
		^res
	}

	key { ^all.findKeyForValue(this) }

	proxy_ { |px|
		if (px.isKindOf(Pdef)) {
			proxy = px;
			// proper init state
			this.currFromProxy;
			currSet = targSet = this.getSet(\curr);
			this.setPath;
		};
	}

	printOn { | stream |
		stream << this.class.name << "(" <<< this.key << ")"
	}
}


NdefPreset : ProxyPreset {
	classvar <all;
	*initClass { all = () }

	*new { |key, settings|
		var res, proxy;
		if (key.isKindOf(NodeProxy)) {
			proxy = key;
			key = proxy.key;
		};
		res = all[key];
		if (res.isNil) {
			"proxy is: ".post;
			res = super.new(proxy, settings);
			all.put(key, res);
		};
		^res
	}

	key { ^all.findKeyForValue(this) }

	proxy_ { |px|
		if (px.isKindOf(NodeProxy)) {
			proxy = px;
		    specs = try { proxy.getSpec } ?? { () };
			this.currFromProxy;
			currSet = targSet = this.getSet(\curr);
			this.setPath;
		} {
			"NdefPreset - can't set proxy to: %!".format(px).warn;
		};
	}

	getFromProxy { |except| ^proxy.getKeysValues(except) }

	setRand { |rand, startSet, except|
		rand = rand ?? { exprand(0.001, 0.25) };
		proxy.set(*this.randSet(rand, startSet, except).flat);
	}

	setCurr { |name|
		var foundSet = this.getSet(name);
		if (foundSet.notNil) {
			currSet = foundSet;
			proxy.set(*currSet.value.flat);
			this.morphVal_(0);
		};
	}

	setProxy { |name| proxy.set(*this.getSet(name).value.flat) }

	morph { |blend, name1, name2, mapped=true|
		proxy.set(*(this.blend(blend, name1, name2, mapped).flat));
		morphVal = blend;
	}

	storeArgs { ^[this.key] }

	printOn { | stream |
		stream << this.class.name << "(" <<< this.key << ")"
	}
}
