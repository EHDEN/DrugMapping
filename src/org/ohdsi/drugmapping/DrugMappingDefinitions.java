package org.ohdsi.drugmapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.ohdsi.drugmapping.files.FileColumnDefinition;
import org.ohdsi.drugmapping.files.FileDefinition;

public class DrugMappingDefinitions {
	
	public static final List<FileDefinition> FILES = new ArrayList<FileDefinition>(
			Arrays.asList(
					new FileDefinition(
							"Replacements File",
							new String[] {
									"This file should contain textual replacements for searching on source texts."
					  		},
							new FileColumnDefinition[] {
									new FileColumnDefinition(
											"Replace",
											new String[] {
													"This is the string to be replaced.",
													"May contain ^ for beginning and $ for ending."
											}
									),
									new FileColumnDefinition(
											"By",
											new String[] {
													"This is the replacement string."
											}
									)
							}
					),
					new FileDefinition(
							"ATC File",
							new String[] {
									"This file should contain the ATC-codes from the source database."
					  		},
							new FileColumnDefinition[] {
									new FileColumnDefinition(
											"ATC",
											new String[] {
													"This is the ATC-code in the source database."
											}
									),
									new FileColumnDefinition(
											"ATCText",
											new String[] {
													"This is the name of the ATC-code",
													"the source database."
											}
									),
									new FileColumnDefinition(
											"ATCTextEnglish",
											new String[] {
													"This is the English translation of the name",
													"of the ATC-code in the source database."
											}
									)
							}
					),
					new FileDefinition(
							"Ingredients File",
							new String[] {
									"This file should contain the ingredients from the source database."
					  		},
							new FileColumnDefinition[] {
									new FileColumnDefinition(
											"IngredientID",
											new String[] {
													"This is the ID of the ingredient in the",
													"source database."
											}
									),
									new FileColumnDefinition(
											"IngredientText",
											new String[] {
													"This is the name of the ingredient in",
													"the source database."
											}
									),
									new FileColumnDefinition(
											"IngredientTextEnglish",
											new String[] {
													"This is the English translation of the name",
													"of the ingredient in the source database."
											}
									)
							}
					)
			)
		);

}
