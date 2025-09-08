package org.fix.repair.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.fix.repair.common.R;
import org.fix.repair.entity.appointments;
import org.fix.repair.mapper.AppointmentsMapper;
import org.fix.repair.service.AppointmentsService;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

@Service
public class AppointmentsServiceImpl extends ServiceImpl <AppointmentsMapper, appointments> implements AppointmentsService {


    @Override
    public R<String> insertAppoint(Map<String, Object> appointInfo) {
        try {
            appointments appoint = new appointments();
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");

            // 解析日期字符串并设置到实体中
            Date appointmentTime = format.parse((String) appointInfo.get("time"));

            appoint.setOpenid((String) appointInfo.get("openid"));
            appoint.setName((String) appointInfo.get("name"));
            appoint.setPhone((String) appointInfo.get("phone"));
            appoint.setTime(appointmentTime); // 使用解析后的Date对象
            appoint.setAddress((String) appointInfo.get("address"));
            appoint.setStatus((String) appointInfo.get("status"));
            appoint.setBrandname((String) appointInfo.get("brandname"));
            appoint.setRepairtype((String) appointInfo.get("repairtype"));
            appoint.setCreatedat(new java.util.Date());

            boolean save = this.save(appoint);
            if (!save) {
                return R.error("保存失败");
            }
            return R.ok("保存成功");

        } catch (ParseException e) {
            return R.error("日期格式错误，请使用 yyyy-MM-dd HH:mm:ss 格式");
        } catch (Exception e) {
            return R.error("系统错误: " + e.getMessage());
        }
    }
}
