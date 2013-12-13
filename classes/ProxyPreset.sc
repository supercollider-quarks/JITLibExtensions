/* to do: 
OK	* store globally by name	
OK	* separate GUI from Preset itself
	* keep local specs here, in Preset? make missing specs?
	* add extras like autorand, autoloop ? 
	* provide timed morphs that can stop / continue
	
	* build into NdefGui as an option, 
	* or do PxPreset(~aproxy); instead of NodeProxyEditor(~aproxy); ? 
	
	// keep the presets next to the text file the proxy/preset was created with;
	// save settings to disk locally ... 
	// else make a preset/settings folder? 
	// use/adapt/generalize for Tdef/Pdef.envir?  

	* how to morph between lists of symbols ... random choice? 
	* how to morph between lists of different lengths? (e.g. pattern value lists)? 
	
*/

		// abstract superclass for NdefPreset, TdefPreset, PdefPreset
ProxyPreset {
	var <proxy, <settings, <currSet, <targSet, count = 0; 
	var <storeToDisk = true, <>storePath; 
	var <>morphVal = 0, <specs, <morphTask, <dt = 0.02, <morphDur = 1;

	*new { |proxy, settings| 
		^super.new.init.proxy_(proxy).addSettings(settings);
	}

	init { 		// a list of assocs
		settings =  List[];
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
		if (toDisk) { this.writeSettings(storePath.splitext.insert(1, "_BK.").join, true) };
		
		if (index.notNil)
			{ 	settings.put(index, name -> values) }
			{ 	settings.add(name -> values) };
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


	keys { ^settings.collect(_.key) }
	
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

	
		// gui stuff; move into its own class? 
		
	storeDialog { |name| 		// check before overwriting a setting?
		var w;
		if (name.isNil, { count = count + 1; name = "set" ++ count; });
		w = GUI.window.new("", Rect(0,600,150, 24), false);
		StaticText(w, Rect(0,0,70,20)).align_(0).string_("name set:");
		GUI.textField.new(w, Rect(70,0,70,20)).align_(0)
			.string_(name)
			.action_({ arg field; 
				this.addSet(field.value.asSymbol, toDisk: storeToDisk); 
				w.close; 
			})
			.focus(true);
		w.front;
	}
			// good enough for now
	deleteDialog {
		var w, names, size, buttons, redraw; 
		names = this.settings.collect(_.key);
		names.remove(\curr); 
		size = names.size; 
		[size, names].postcs;
		
			// up arr, down arr, delete, listview; 
		w = Window("delete which?", Rect(0,600, 200, 400));
		w.view.decorator = FlowLayout(Rect(0,0, 200, 400));
		
		buttons = names.collect { |setname|
			Button(w, Rect(0,0,96, 20))
				.states_([[setname.asString]])
				.action_({ |btn| 
					this.removeSet(btn.states.first.first.asSymbol.postcs);
					redraw.value;
				});
		};
		
		redraw = { 
			names = this.settings.collect(_.key);
			names.remove(\curr); 
			buttons.do { |btn, i| 
				btn.states_([[names[i] ? '_']])
			};
			w.refresh;
		};
		w.front;
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
						// proper init state
			this.currFromProxy; 
			currSet = targSet = this.getSet(\curr);
			this.setPath;
		};
	}	

	getFromProxy { |except| 
		var envir = proxy.envir;
		var settings = List.new; 
		if (envir.notNil) {
			envir.keys.difference(except).asArray.sort.do { |key| 
				settings.add([key, envir[key]]);	
			};
		};
		^envir
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
	getFromProxy { |except| 
		var envir = proxy.envir;
		var settings = List.new; 
		if (envir.notNil) {
			envir.keys.difference(except).asArray.sort.do { |key| 
				settings.add([key, envir[key]]);	
			};
		};
		^envir
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
		if (px.isKindOf(NodeProxy).postcs) { 
			proxy = px; 
			this.currFromProxy; 
			currSet = targSet = this.getSet(\curr);
			this.setPath;
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
}

ProxyPresetGui : JITGui {

	var <setLBox, <setLPop, <storeBtn, <delBtn, <setRPop, <setRBox, <xfader;

	setDefaults { 
		defPos = 10@10;
		minSize = 400 @ 52;
	}
	
	makeViews {
		var butHeight = skin.headHeight; 
		var flow = zone.decorator;

		StaticText(zone, Rect(0,0, 40, butHeight)).string_("curr")
			.background_(skin.foreground)
			.font_(font).align_(0);

		setLPop = PopUpMenu(zone, Rect(0,0, 80, butHeight))
			.items_([]).font_(font)
			.background_(skin.foreground)
			.action_({ |pop| var name; 
				name = pop.items[pop.value].asSymbol;
				object.setProxy(name); 		// sets proxy too.
				object.setCurr(name);
				object.morphVal_(0);
			});
			
		storeBtn = Button(zone, Rect(0,0, 40, butHeight))
			.states_([["sto", skin.fontColor, skin.foreground]])
			.font_(font)
			
			.action_({ object.storeDialog });
		delBtn =  Button(zone, Rect(0,0, 40, butHeight))
			.states_([["del", skin.fontColor, skin.foreground]]).font_(font)
			.action_({ object.deleteDialog });
		
		 Button(zone, Rect(0,0,40, butHeight))
			.states_([["rand", skin.fontColor, skin.foreground]]).font_(font)
			.action_({ |but, modif|
					// cocoa and swingosc -alt mod.
				var rand = if ([524576, 24].includes(modif)) { object.setRand(1.0) } { object.setRand  };
				
			});
			
		 Button(zone, Rect(0,0, 40, butHeight))
			.states_([["doc", skin.fontColor, skin.foreground]]).font_(font)
			.action_({ object.postSettings });
		
		setRPop = PopUpMenu(zone, Rect(0,0, 80, butHeight))
			.items_([]).font_(font)
			.background_(skin.foreground)
			.action_({ |pop| 
				object.setTarg(pop.items[pop.value].asSymbol); 
				object.morphVal_(0);
			});

		 StaticText(zone, Rect(0,0, 40, butHeight))
			.background_(skin.foreground)
			.string_("targ").font_(font).align_(0);
		
		flow.nextLine;
		
		setLBox = NumberBox(zone, Rect(0,0, 25, butHeight))
			.background_(skin.foreground)
			.font_(font).align_(0).value_(-1)
			.action_({ |box| 
				var val = box.value % setLPop.items.size; 
				setLPop.valueAction_(val);
				box.value_(val)
			});
			
		xfader = Slider(zone, Rect(0,0, 350, butHeight))
			.action_({ |sl| 
				object.morph(sl.value, object.currSet.key, object.targSet.key); 
			});
			
		setRBox = NumberBox(zone, Rect(0,0, 25, butHeight))
			.background_(skin.foreground)
			.font_(font).align_(0).value_(-1)
			.action_({ |box| 
				var val = box.value % setRPop.items.size; 
				setRPop.valueAction_(val);
				box.value_(val)
			});
		
	}
	getState { 
		if (object.isNil) { 
			^(setNames: [], morphVal: 0, morphDur: 1);
		};
		
		^(	object: object, 
			setNames: object.settings.collect(_.key), 
			currSet: object.currSet, 
			currIndex: object.currIndex, 
			targSet: object.targSet, 
			targIndex: object.targIndex,
			morphVal: object.morphVal, 
			morphDur: object.morphDur
		)
	}
	checkUpdate { 
		var newState = this.getState;
		
		if (prevState[\object] != newState[\object]) {
			zone.enabled_(object.notNil);	
		};

		if (prevState[\setNames] != newState[\setNames]) {
			setLPop.items = newState[\setNames];
			setRPop.items = newState[\setNames];
		};
		if (prevState[\currIndex] != newState[\currIndex]) {
			setLPop.value = newState[\currIndex];
			setLBox.value = newState[\currIndex];
			
		};
		if (prevState[\targIndex] != newState[\targIndex]) {
			setRPop.value = newState[\targIndex];
			setRBox.value = newState[\targIndex];
		};

		if (prevState[\morphVal] != newState[\morphVal]) {
			xfader.value_(newState[\morphVal])
		};
	}
}