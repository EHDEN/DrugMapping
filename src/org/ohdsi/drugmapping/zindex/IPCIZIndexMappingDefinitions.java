package org.ohdsi.drugmapping.zindex;

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
									"This file should contain the ZIndex GNK ingredient definitions."
					  		},
							new FileColumnDefinition[] {
									new FileColumnDefinition(
											"GNKCode",
											new String[] {
													"This is the GNK-code."
											}
									),
									new FileColumnDefinition(
											"Description",
											new String[] {
													"This is the description for the GNK-code."
											}
									),
									new FileColumnDefinition(
											"CASCode",
											new String[] {
													"This is the CAS number of the ingredient."
											}
									)
							}
					),
					new FileDefinition(
							"ZIndex GPK Statistics File",
							new String[] {
									"This file should contain the usage counts of the ZIndex GPK codes."
					  		},
							new FileColumnDefinition[] {
									new FileColumnDefinition(
											"GPKCode",
											new String[] {
													"This is the GPK-code."
											}
									),
									new FileColumnDefinition(
											"GPKCount",
											new String[] {
													"This is the usage count for the GPK-code."
											}
									)
							}
					),
					new FileDefinition(
							"ZIndex Ignored Words File",
							new String[] {
									"This file should contain words to remove from ingredients extracted.",
					  		},
							new FileColumnDefinition[] {
									new FileColumnDefinition(
											"Word",
											new String[] {
													"This is the word to remove."
											}
									)
							}
					)
			)
		);

}
