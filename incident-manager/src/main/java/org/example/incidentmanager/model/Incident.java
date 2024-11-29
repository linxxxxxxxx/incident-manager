package org.example.incidentmanager.model;

import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Incident implements Serializable {

    private Long id;

    @NotBlank(message = "Name is required")
    @Size(min = 1, max = 50, message = "The length of the incident name should be between 1 and 50 characters")
    private String name;

    // 添加非空和长度限制校验注解，描述不能为空且长度在1到200之间
    @NotBlank(message = "Description is required")
    @Size(min = 1, max = 200, message = "The length of the incident description should be between 1 and 200 characters")
    private String description;

    private Date createdDate;

    private Date updatedDate;

}
