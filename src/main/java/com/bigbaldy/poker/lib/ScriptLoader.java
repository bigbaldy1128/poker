package com.bigbaldy.poker.lib;

import com.google.common.io.Files;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Component
public class ScriptLoader implements InitializingBean {
    private Map<String, String> scriptMap;
    private Map<String, String> painlessMap;

    @Value("classpath*:lua/*.lua")
    private Resource[] luaFiles;

    @Value("classpath*:painless/*.painless")
    private Resource[] painlessFiles;

    @Override
    public void afterPropertiesSet() throws IOException {
        scriptMap = new HashMap<>();
        painlessMap = new HashMap<>();
        for (Resource luaFile : luaFiles) {
            String content = IOUtils.toString(luaFile.getInputStream(), StandardCharsets.UTF_8);
            scriptMap.put(Files.getNameWithoutExtension(luaFile.getFilename()), content);
        }
        for (Resource painlessFile : painlessFiles) {
            String content = IOUtils.toString(painlessFile.getInputStream(), StandardCharsets.UTF_8);
            painlessMap.put(Files.getNameWithoutExtension(painlessFile.getFilename()), content);
        }
    }

    public String getScript(String scriptName) {
        return scriptMap.get(scriptName);
    }

    public String getPainlessScript(String scriptName) {
        return painlessMap.get(scriptName);
    }
}
