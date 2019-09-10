package org.ohdsi.drugmapping.genericmapping;

import java.util.ArrayList;
import java.util.Arrays;
import org.ohdsi.drugmapping.MappingInputDefinition;
import org.ohdsi.drugmapping.files.FileColumnDefinition;
import org.ohdsi.drugmapping.files.FileDefinition;

public class GenericMappingInputFiles extends MappingInputDefinition {

	
	public GenericMappingInputFiles() {
		inputFiles = new ArrayList<FileDefinition>(
				Arrays.asList(
						new FileDefinition(
								"Generic Drugs File",
								new String[] {
										"This file should contain the genric drugs from the source database."
						  		},
								new FileColumnDefinition[] {
										new FileColumnDefinition(
												"SourceCode",
												new String[] {
														"This is the GPK-code of the drug."
												}
										),
										new FileColumnDefinition(
												"SourceName",
												new String[] {
														"This is the full name of the drug."
												}
										),
										new FileColumnDefinition(
												"SourceATCCode",
												new String[] {
														"This is the ATC-code of the drug."
												}
										),
										new FileColumnDefinition(
												"SourceFormulation",
												new String[] {
														"This is the pharmaceutical form of the drug."
												}
										),
										new FileColumnDefinition(
												"SourceCount",
												new String[] {
														"The number of occurrences of the source drug in the source database."
												}
										),
										new FileColumnDefinition(
												"IngredientName",
												new String[] {
														"This is the name of the ingredient of the drug."
												}
										),
										new FileColumnDefinition(
												"IngredientNameEnglish",
												new String[] {
														"This is the English name of the ingredient of the drug."
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
												"CASNumber",
												new String[] {
														"This is the CAS number of the",
														"ingredient in this combination."
												}
										)
								}
						)/*,
						new FileDefinition(
								"Manual Translation File",
								new String[] {
										"This file should contain corrections on translations in the Genric Drugs File.",
										"This file is optional."
						  		},
								new FileColumnDefinition[] {
										new FileColumnDefinition(
												"IngredientName",
												new String[] {
														"This is the name of an ingredient."
												}
										),
										new FileColumnDefinition(
												"IngredientNameEnglish",
												new String[] {
														"This is the English name of the ingredient."
												}
										)
								}
						)*/
				)
			);
	}
}
