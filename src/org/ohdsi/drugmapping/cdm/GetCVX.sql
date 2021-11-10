SELECT *
FROM  @vocab.concept
WHERE concept_class_id = 'CVX'
AND   standard_concept = 'S';