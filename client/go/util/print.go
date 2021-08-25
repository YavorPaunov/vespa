// Copyright Verizon Media. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
// Print functions for color-coded text.
// Author: bratseth

package util

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"os"
)

// Set this to have output written somewhere else than os.Stdout
var Out io.Writer

func init() {
	Out = os.Stdout
}

// Prints in default color
func Print(messages ...string) {
	print("", messages)
}

// Prints in a color appropriate for errors
func Error(messages ...string) {
	print("\033[31m", messages)
}

// Prints in a color appropriate for success messages
func Success(messages ...string) {
	print("\033[32m", messages)
}

// Prints in a color appropriate for detail messages
func Detail(messages ...string) {
	print("\033[33m", messages)
}

// Prints all the text of the given reader
func PrintReader(reader io.Reader) {
	bodyBytes := ReaderToBytes(reader)
	var prettyJSON bytes.Buffer
	parseError := json.Indent(&prettyJSON, bodyBytes, "", "    ")
	if parseError != nil { // Not JSON: Print plainly
		Print(string(bodyBytes))
	} else {
		Print(string(prettyJSON.Bytes()))
	}
}

func print(prefix string, messages []string) {
	fmt.Fprint(Out, prefix)
	for i := 0; i < len(messages); i++ {
		fmt.Fprint(Out, messages[i])
		if i < len(messages)-1 {
			fmt.Fprint(Out, " ")
		}
	}
	fmt.Fprintln(Out, "")
}