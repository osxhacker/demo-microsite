htmx.defineExtension (
    'parsley-validation',
    {
        onEvent : function (name, evt) {
            var allow = true;

            if (name === 'htmx:load')
                $(evt.target).parsley ();
            else if (name === 'htmx:confirm') {
                var theForm = $(evt.target).parsley ();

                theForm.validationResult = allow = theForm.isValid ();

                if (!allow)
                    evt.preventDefault ();
            }

            return allow;
        }
    }
)

