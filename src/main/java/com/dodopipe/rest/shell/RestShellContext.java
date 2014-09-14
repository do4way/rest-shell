package com.dodopipe.rest.shell;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.rest.shell.commands.ContextCommands;
import org.springframework.expression.PropertyAccessor;
import org.springframework.stereotype.Component;

/**
 * @author <a href="mailto:yongwei.dou@gmail.com">Yongwei Dou</a>
 */
@Component
public class RestShellContext {

    @Autowired
    private ContextCommands contextCommands;

    public void setContextVariable(String name,
                                   Object value) {

        contextCommands.variables.put(name,
                           value);
    }

    public Object getContextVariable(String name) {

        return contextCommands.variables.get(name);
    }

    public void addPropertyAccessor(PropertyAccessor accessor) {

        contextCommands.evalCtx.addPropertyAccessor(accessor);
    }

    public Object eval(String expr) {

        return contextCommands.eval(expr);
    }

    public String evalAsString(String expr) {

        return contextCommands.evalAsString(expr);
    }
}
