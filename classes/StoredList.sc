StoredList {
	classvar <>verbose = false;

	var <id, <list, <>suffix, <>fileExt = ".scd";
	var <>dirname = "settings", <>dirpath;
	var <>addToDisk = false;

	*new { |id, list, suffix = ".pxpreset"|
		^super.newCopyArgs(id, list ?? { List[] }, suffix);
	}

	storeArgs { ^[id] }
	printOn { |stream| ^this.storeOn(stream) }

	at { |nameOrIndex|
		if (nameOrIndex.isArray) {
			^nameOrIndex.collect(this.at(_))
		};
		if (nameOrIndex.isNumber) {
			^list[nameOrIndex]
		};
		^list.detect { |assoc|
			assoc.key == nameOrIndex
		}
	}

	names { ^list.collect (_.key) }
	values { ^list.collect (_.value) }


	indexOf { |name| ^list.detectIndex { |assoc| assoc.key == name } }

	add { |name, setting|
		var foundIndex = this.indexOf(name);
		if (foundIndex.notNil and: { setting.notNil }) {
			list.put(foundIndex, name -> setting)
		} {
			list.add(name -> setting)
		};
		if (addToDisk) { this.write };
	}

	removeAt { |name|
		var foundItem = this.at(name);
		list.remove(foundItem);
		^foundItem
	}

	////// storage ////////
	local {
		var localpath = thisProcess.nowExecutingPath;
		if (localpath.isNil) {
			"*** % cannot store at nowExecutingPath!".postf(this);
			this.dir;
		};
		"*** % : storing at %\n".postf(this.dir.cs);
	}

	filename { ^id ++ suffix ++ fileExt }

	dir {
		dirpath = dirpath ?? { Platform.userAppSupportDir };
		^this.dirpath +/+ dirname
	}

	backupDir { ^this.dir +/+ "zzz" }

	// refine later
	prettyList {
		^"List[\n\t"
		++ list.collect(_.cs).join(",\n\t")
		++ "\n]"
	}

	storePath { ^this.dir +/+ this.filename }

	write { |path|
		File.mkdir(this.dir);
		path = path ?? { this.storePath };
		this.backup(path);
		File.use(path, "w", {|file|
			if (file.isOpen) {
				file.write(this.prettyList);
				file.close;
				"%: stored % settings at: \n%\n".postf(this, list.size, path.cs);
			} {
				"%: file store failed...".postln;
			}
		});
	}

	read { |path, keep = false, doneFunc|
		var newList;
		path = path ?? { this.storePath };
		newList = path.standardizePath.load;
		if (newList.isKindOf(List)) {
			"% read % settings from: \n%\n".postf(this, newList.size, path.cs);
			if (keep) {
				list.addAll(newList)
			} {
				list = newList;
			};
			doneFunc.value(this)
		}
	}

	backup { |path|
		var backupFilename, backupDir;
		var extensions = this.filename.drop(id.asString.size);
		path = path ?? { this.storePath };
		backupFilename = [ id, Date.getDate.stamp, "BK", extensions].join("_");
		backupDir = this.backupDir;
		File.mkdir(backupDir);
		unixCmd("cp" + quote(path) + quote(backupDir +/+ backupFilename));
	}
}
