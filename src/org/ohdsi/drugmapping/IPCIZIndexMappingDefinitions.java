package org.ohdsi.drugmapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.ohdsi.drugmapping.files.FileColumnDefinition;
import org.ohdsi.drugmapping.files.FileDefinition;

public class IPCIZIndexMappingDefinitions {
	
	public static final List<FileDefinition> FILES = new ArrayList<FileDefinition>(
			Arrays.asList(
					new FileDefinition(
							"ZIndex GPK File",
							new String[] {
									"This file should contain ZIndex GPK drug definitions."
					  		},
							new FileColumnDefinition[] {
									new FileColumnDefinition(
											"GPKCode",
											new String[] {
													"This is the GPK-code of the drug."
											}
									),
									new FileColumnDefinition(
											"MemoCode",
											new String[] {
													"This is the memo code of the drug."
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
											"GSKCode",
											new String[] {
													"This is the GSK-code of the drug."
											}
									),
									new FileColumnDefinition(
											"DDDPerHPKUnit",
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
											"HPKMG",
											new String[] {
													"This is the dosage in mg of the drug."
											}
									),
									new FileColumnDefinition(
											"HPKMGUnit",
											new String[] {
													"This is the mg unit of the drug."
											}
									),
									new FileColumnDefinition(
											"PharmForm",
											new String[] {
													"This is the pharmaceutical form of the drug."
											}
									)
							}
					),
					new FileDefinition(
							"ZIndex GSK File",
							new String[] {
									"This file should contain the ZIndex GSK (component) definitions."
					  		},
							new FileColumnDefinition[] {
									new FileColumnDefinition(
											"GSKCode",
											new String[] {
													"This is the GSK-code of the combination."
											}
									),
									new FileColumnDefinition(
											"PartNumber",
											new String[] {
													"This is the part number of the component",
													"in this combination."
											}
									),
									new FileColumnDefinition(
											"Type",
											new String[] {
													"This is the type of the component",
													"in this combination."
											}
									),
									new FileColumnDefinition(
											"Amount",
											new String[] {
													"This is the amount of the component",
													"in this combination."
											}
									),
									new FileColumnDefinition(
											"AmountUnit",
											new String[] {
													"This is the unit of the amount of the",
													"component in this combination."
											}
									),
									new FileColumnDefinition(
											"GNKCode",
											new String[] {
													"This is the GNKCode of the component",
													"in this combination."
											}
									),
									new FileColumnDefinition(
											"GenericName",
											new String[] {
													"This is the generic name of the",
													"component in this combination."
											}
									),
									new FileColumnDefinition(
											"CASNumber",
											new String[] {
													"This is the CAS number of the",
													"component in this combination."
											}
									)
							}
					),
					new FileDefinition(
							"ZIndex GNK File",
							new String[] {
									"This file should contain the substances of the drug components."
					  		},
							new FileColumnDefinition[] {
									new FileColumnDefinition(
											"GNKCode",
											new String[] {
													"This is the GNK-code of the substance."
											}
									),
									new FileColumnDefinition(
											"Description",
											new String[] {
													"This is the description of the substance."
											}
									),
									new FileColumnDefinition(
											"CASCode",
											new String[] {
													"This is the CAS number of the substance."
											}
									)
							}
					)
			)
		);

}
