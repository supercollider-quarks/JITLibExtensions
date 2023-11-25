+ ProxyPreset {

	postSettings {
		Document(proxy.key ++ ".pxpreset.scd",
		this.settingsString);
	}

	openSettingsFile {
		var foundfile = this.storePath.pathMatch.first;
		if (foundfile.notNil) {
			Document.open(foundfile);
		} {
			"%: no file at storePath, putting settings into document.".postln;
			this.postSettings
		}
	}

	storeDialog { |name, loc| 		// check before overwriting a setting?
		var w;
		loc = loc ?? {400@300};
		name = this.checkName(name);
		w = Window("", Rect(loc.x, loc.y - 40, 150, 40), false);
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

		names = this.getSetNames;
		names.remove(\curr);
		loc = loc ?? { (100@400) };

		win = Window("delete", Rect(loc.x, loc.y - 240, 150, 200)).front;
		win.addFlowLayout;
		ezlist = EZListView(win, win.bounds.insetBy(4, 4),
			"DELETE presets from\n%-"
			"\nselect and type D:".format(this),
			names, nil, labelHeight: 50);
		ezlist.labelView.align_(\center);
		ezlist.view.resize_(5);
		ezlist.widget.resize_(5);
		ezlist.widget.action = { |view|
			this.setCurr(view.item);
		};
		ezlist.widget.keyDownAction_({ |view, char|
			var prevVal = view.value;
			if(char.toLower == $d) {
				this.removeSet(view.items[view.value].postln);
				view.items = this.getSetNames;
				view.value = prevVal;
			};
		});
		^win
	}

	specsDialog { |keys, specDict|

		var w, loc, name, proxyKeys, specKeys;
		specDict = specDict ? specs;

		loc = loc ?? {400@300};
		w = Window("specs please", Rect(loc.x, loc.y + 40, 300, 200)).front;
		w.addFlowLayout;
		StaticText(w, Rect(0,0,290,50)).align_(\center)
		.string_(
			"Please enter specs for the following\nparameter keys:"
			"\n(min, max, warp, step, default, units)"
		);

		keys.collect { |key|
			var guessedSpec = Spec.guess(key, proxy.get(key)).storeArgs;
			var eztext;
			eztext = EZText(w, Rect(70,0,290,20), key, { |ez|
				var spec = ez.value.asSpec;
				specDict.put(key, spec);
				[key, spec].postcs;
			},
			guessedSpec
			);
		};
	}
}