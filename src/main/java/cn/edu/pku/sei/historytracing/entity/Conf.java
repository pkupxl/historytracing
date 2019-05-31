package cn.edu.pku.sei.historytracing.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties
public class Conf {
    @Getter
    @Setter
    private String infoDir;

}