test:
	pytest --cov-report term-missing --cov=swatchblade tests/ -vv
unit-test:
	pytest --cov-report term-missing --cov=swatchblade tests/ -vv -m "not integration_test"
typecheck:
	mypy swatchblade
format:
	black swatchblade tests
format-check:
	black --check swatchblade tests
