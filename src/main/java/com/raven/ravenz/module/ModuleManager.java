package com.raven.ravenz.module;

import com.raven.ravenz.module.modules.client.*;
import com.raven.ravenz.module.modules.combat.AutoCrystal;
import com.raven.ravenz.module.modules.combat.ShieldBreaker;
import com.raven.ravenz.module.modules.combat.TriggerBot;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Getter
public final class ModuleManager {

    private final List<Module> modules = new java.util.ArrayList<>();

    public ModuleManager() {
        addModules();
    }

    public List<Module> getEnabledModules() {
        return modules.stream()
                .filter(Module::isEnabled)
                .toList();
    }

    public List<Module> getModulesInCategory(Category category) {
        return modules.stream()
                .filter(module -> module.getModuleCategory() == category)
                .toList();
    }

    public List<Module> getModulesByCategory(Category category) {
        return getModulesInCategory(category);
    }

    public <T extends Module> Optional<T> getModule(Class<T> moduleClass) {
        return modules.stream()
                .filter(module -> module.getClass().equals(moduleClass))
                .map(moduleClass::cast)
                .findFirst();
    }

    private void addModules() {
        // Combat
        add(
                new AutoCrystal(),
                new ShieldBreaker()
        );

        // Player
        add(
                new TriggerBot()
        );

        // Client
        add(
                new ClickGUIModule(),
                new NewClickGUIModule(),
                new RichModernGUIModule(),
                new ClientSettingsModule(),
                new Client(),
                new Debugger(),
                new Secret(),
                new KeybindsModule()
        );
    }

    private void add(Module... mods) {
        modules.addAll(Arrays.asList(mods));
    }
}
