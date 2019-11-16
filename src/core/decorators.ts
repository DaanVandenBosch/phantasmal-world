import Logger = require("js-logger");

const logger = Logger.get("core/decorators");

/**
 * Prints a warning when the method is called.
 */
export const stub: MethodDecorator = function stub(
    target: Record<string, any>,
    prop_key: PropertyKey,
    descriptor: PropertyDescriptor,
) {
    const orig_method: Function = descriptor.value;

    descriptor.value = function(...args: any[]): any {
        logger.warn(`Stub: ${target.constructor.name}.prototype.${String(prop_key)}`);
        return orig_method.apply(this, args);
    };

    return descriptor;
};
