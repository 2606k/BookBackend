package org.fix.repair.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.fix.repair.common.R;
import org.fix.repair.entity.appointments;

import java.util.Map;

public interface AppointmentsService extends IService<appointments> {
    R insertAppoint(Map<String, Object> appointInfo);
}
