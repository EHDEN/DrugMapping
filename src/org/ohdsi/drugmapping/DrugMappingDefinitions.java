package org.ohdsi.drugmapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.ohdsi.drugmapping.files.FileColumnDefinition;
import org.ohdsi.drugmapping.files.FileDefinition;

public class DrugMappingDefinitions {
	
	public static final List<FileDefinition> FILES = new ArrayList<FileDefinition>(
			Arrays.asList(
/*
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
					),
*/
					new FileDefinition(
							"Generic Drugs File",
							new String[] {
									"This file should contain the genric drugs from the source database."
					  		},
							new FileColumnDefinition[] {
									new FileColumnDefinition(
											"GenericDrugCode",
											new String[] {
													"This is the GPK-code of the drug."
											}
									),
									new FileColumnDefinition(
											"LabelName",
											new String[] {
													"This is the label of the drug."
											}
									),
									new FileColumnDefinition(
											"ShortName",
											new String[] {
													"This is the short name of the drug."
											}
									),
									new FileColumnDefinition(
											"FullName",
											new String[] {
													"This is the full name of the drug."
											}
									),
									new FileColumnDefinition(
											"ATCCode",
											new String[] {
													"This is the ATC-code of the drug."
											}
									),
									new FileColumnDefinition(
											"DDDPerUnit",
											new String[] {
													"This is the DDD per HPK unit of the drug."
											}
									),
									new FileColumnDefinition(
											"PrescriptionDays",
											new String[] {
													"This is the normal prescription length (days)",
													"of the drug."
											}
									),
									new FileColumnDefinition(
											"Dosage",
											new String[] {
													"This is the dosage in mg of the drug."
											}
									),
									new FileColumnDefinition(
											"DosageUnit",
											new String[] {
													"This is the mg unit of the drug."
											}
									),
									new FileColumnDefinition(
											"PharmaceuticalForm",
											new String[] {
													"This is the pharmaceutical form of the drug."
											}
									),
									new FileColumnDefinition(
											"ComponentCode",
											new String[] {
													"This is the GSK-code of the drug."
											}
									),
									new FileColumnDefinition(
											"ComponentPartNumber",
											new String[] {
													"This is the part number of the component",
													"in this combination."
											}
									),
									new FileColumnDefinition(
											"ComponentType",
											new String[] {
													"This is the type of the component",
													"in this combination."
											}
									),
									new FileColumnDefinition(
											"ComponentAmount",
											new String[] {
													"This is the amount of the component",
													"in this combination."
											}
									),
									new FileColumnDefinition(
											"ComponentAmountUnit",
											new String[] {
													"This is the unit of the amount of the",
													"component in this combination."
											}
									),
									new FileColumnDefinition(
											"ComponentGenericName",
											new String[] {
													"This is the generic name of the",
													"component in this combination."
											}
									),
									new FileColumnDefinition(
											"ComponentCASNumber",
											new String[] {
													"This is the CAS number of the",
													"component in this combination."
											}
									),
									new FileColumnDefinition(
											"SubstanceCode",
											new String[] {
													"This is the GNKCode of the component",
													"in this combination."
											}
									),
									new FileColumnDefinition(
											"SubstanceDescription",
											new String[] {
													"This is the description of the substance."
											}
									),
									new FileColumnDefinition(
											"SubstanceCASNumber",
											new String[] {
													"This is the CAS number of the substance."
											}
									)
							}
					)
			)
		);

}