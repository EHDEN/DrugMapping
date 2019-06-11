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
							"Units Mapping File",
							new String[] {
									"This file should contain the mappings from the local units.",
									"to CDM unit concepts with a multiplcation factor."
					  		},
							new FileColumnDefinition[] {
									new FileColumnDefinition(
											"LocalUnit",
											new String[] {
													"This is name of the unit in the",
													"source database."
											}
									),
									new FileColumnDefinition(
											"concept_id_1",
											new String[] {
													"This is the first CDM unit concept",
													"the unit is mapped to."
											}
									),
									new FileColumnDefinition(
											"factor_1",
											new String[] {
													"This is the factor by which CDM values of",
													"the first CDM unit should be multiplied."
											}
									),
									new FileColumnDefinition(
											"concept_id_2",
											new String[] {
													"This column is optional.",
													"This is the second CDM unit concept",
													"the unit is mapped to."
											}
									),
									new FileColumnDefinition(
											"factor_2",
											new String[] {
													"This optional but should be present when",
													"the column concept_id_2 is present.",
													"This is the factor by which CDM values of",
													"the second CDM unit should be multiplied."
											}
									),
									new FileColumnDefinition(
											"concept_id_3",
											new String[] {
													"This column is optional.",
													"This is the third CDM unit concept",
													"the unit is mapped to."
											}
									),
									new FileColumnDefinition(
											"factor_3",
											new String[] {
													"This optional but should be present when",
													"the column concept_id_3 is present.",
													"This is the factor by which CDM values of",
													"the third CDM unit should be multiplied."
											}
									),
									new FileColumnDefinition(
											"concept_id_4",
											new String[] {
													"This column is optional.",
													"This is the fourth CDM unit concept",
													"the unit is mapped to."
											}
									),
									new FileColumnDefinition(
											"factor_4",
											new String[] {
													"This optional but should be present when",
													"the column concept_id_4 is present.",
													"This is the factor by which CDM values of",
													"the fourth CDM unit should be multiplied."
											}
									),
									new FileColumnDefinition(
											"concept_id_5",
											new String[] {
													"This column is optional.",
													"This is the fifth CDM unit concept",
													"the unit is mapped to."
											}
									),
									new FileColumnDefinition(
											"factor_5",
											new String[] {
													"This optional but should be present when",
													"the column concept_id_5 is present.",
													"This is the factor by which CDM values of",
													"the fifth CDM unit should be multiplied."
											}
									),
									new FileColumnDefinition(
											"concept_id_6",
											new String[] {
													"This column is optional.",
													"This is the sixth CDM unit concept",
													"the unit is mapped to."
											}
									),
									new FileColumnDefinition(
											"factor_6",
											new String[] {
													"This optional but should be present when",
													"the column concept_id_6 is present.",
													"This is the factor by which CDM values of",
													"the sixth CDM unit should be multiplied."
											}
									),
									new FileColumnDefinition(
											"concept_id_7",
											new String[] {
													"This column is optional.",
													"This is the seventh CDM unit concept",
													"the unit is mapped to."
											}
									),
									new FileColumnDefinition(
											"factor_7",
											new String[] {
													"This optional but should be present when",
													"the column concept_id_7 is present.",
													"This is the factor by which CDM values of",
													"the seventh CDM unit should be multiplied."
											}
									),
									new FileColumnDefinition(
											"concept_id_8",
											new String[] {
													"This column is optional.",
													"This is the eighth CDM unit concept",
													"the unit is mapped to."
											}
									),
									new FileColumnDefinition(
											"factor_8",
											new String[] {
													"This optional but should be present when",
													"the column concept_id_8 is present.",
													"This is the factor by which CDM values of",
													"the eighth CDM unit should be multiplied."
											}
									),
									new FileColumnDefinition(
											"concept_id_9",
											new String[] {
													"This column is optional.",
													"This is the ninth CDM unit concept",
													"the unit is mapped to."
											}
									),
									new FileColumnDefinition(
											"factor_9",
											new String[] {
													"This optional but should be present when",
													"the column concept_id_9 is present.",
													"This is the factor by which CDM values of",
													"the ninth CDM unit should be multiplied."
											}
									),
									new FileColumnDefinition(
											"concept_id_10",
											new String[] {
													"This is the fifth CDM unit concept",
													"the unit is mapped to."
											}
									),
									new FileColumnDefinition(
											"factor_10",
											new String[] {
													"This optional but should be present when",
													"the column concept_id_10 is present.",
													"This is the factor by which CDM values of",
													"the fifth CDM unit should be multiplied."
											}
									)
							}
					),
					new FileDefinition(
							"Forms Mapping File",
							new String[] {
									"This file should contain the mappings from the local",
									"pharmaceutical form to the CDM pharmaceutical form."
					  		},
							new FileColumnDefinition[] {
									new FileColumnDefinition(
											"LocalForm",
											new String[] {
													"This is the text to match the PharmaceuticalForm",
													"column in the generic drugs file."
											}
									),
									new FileColumnDefinition(
											"CDMForm",
											new String[] {
													"This is the text to match the concept_name",
													"of the CDM pharmaceutical form."
											}
									)
							}
					),
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
													"This is the DDD per unit of the drug."
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
											"IngredientCode",
											new String[] {
													"This is the ID of the ingredient of the drug."
											}
									),
									new FileColumnDefinition(
											"IngredientPartNumber",
											new String[] {
													"This is the part number of the ingredient",
													"in this combination."
											}
									),
									new FileColumnDefinition(
											"IngredientAmount",
											new String[] {
													"This is the amount of the ingredient",
													"in this combination."
											}
									),
									new FileColumnDefinition(
											"IngredientAmountUnit",
											new String[] {
													"This is the unit of the amount of the",
													"ingredient in this combination."
											}
									),
									new FileColumnDefinition(
											"IngredientGenericName",
											new String[] {
													"This is the generic name of the",
													"ingredient in this combination."
											}
									),
									new FileColumnDefinition(
											"IngredientCASNumber",
											new String[] {
													"This is the CAS number of the",
													"ingredient in this combination."
											}
									),
									new FileColumnDefinition(
											"SubstanceCode",
											new String[] {
													"This is the ID of the ingredient substance",
													"in this combination."
											}
									),
									new FileColumnDefinition(
											"SubstanceDescription",
											new String[] {
													"This is the description of the ingredient substance."
											}
									)
							}
					)
			)
		);

}
