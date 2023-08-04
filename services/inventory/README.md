Inventory Microservice
=============================

This sub-project will define the `inventory` microservice.  Until the implementation has begun, it serves to verify the [g8 microservice template](../template.g8/README.md) thusly:


```
# Ensure any generated artifacts which may have been deleted
# in the template no longer exist.
cd $(git rev-parse --show-toplevel)/services/inventory &&
	rm -r src target 2>/dev/null

# Generate source artifacts using the latest version of the
# templates.
cd $(git rev-parse --show-toplevel)/services &&
	g8 "file://$PWD/template.g8/" --name="Inventory"
```

