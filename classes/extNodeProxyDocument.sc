+ NodeProxy {
	accessStr { | envir |
		var accessStr = "a", nameStr, isAnon;

		envir = envir ? currentEnvironment;

		nameStr = envir.use { this.asCompileString };
		isAnon = nameStr.beginsWith("a = ");
		if (isAnon.not) { accessStr = nameStr };

		^accessStr;
	}

	currentSettingsAsCompileString {
		^this.nodeMap.asCode(this.accessStr, true);
	}

}