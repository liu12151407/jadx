plugins {
	id("jadx-library")
}

dependencies {
	api(project(":jadx-core"))

	// show bytecode disassemble
	implementation("io.github.skylot:raung-disasm:0.1.0")

	testImplementation(project(":jadx-core"))
}
