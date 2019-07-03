package dk.itu.pac.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;

@CrossOrigin
@RestController
@RequestMapping(value = "pac/v1", produces = MediaType.APPLICATION_JSON_VALUE)
public class PacController {

    private static final Logger logger = LoggerFactory.getLogger(PacController.class);


    private volatile String ip="";

    @Value("#{'${list.pwd}'.split(',')}")
    private List<String> pwds;

    @RequestMapping(value = "/ip", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public String getIPInfo(HttpServletRequest request,@RequestBody String pwd) {
        if("Xiao1989".equals(pwd)){
            String ipAddress = null;
            try {
                ipAddress = request.getHeader("x-forwarded-for");
                if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
                    ipAddress = request.getHeader("Proxy-Client-IP");
                }
                if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
                    ipAddress = request.getHeader("WL-Proxy-Client-IP");
                }
                if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
                    ipAddress = request.getRemoteAddr();
                    if (ipAddress.equals("127.0.0.1")) {
                        // 根据网卡取本机配置的IP
                        InetAddress inet = null;
                        try {
                            inet = InetAddress.getLocalHost();
                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                        }
                        ipAddress = inet.getHostAddress();
                    }
                }
                // 对于通过多个代理的情况，第一个IP为客户端真实IP,多个IP按照','分割
                if (ipAddress != null && ipAddress.length() > 15) { // "***.***.***.***".length()
                    // = 15
                    if (ipAddress.indexOf(",") > 0) {
                        ipAddress = ipAddress.substring(0, ipAddress.indexOf(","));
                    }
                }
            } catch (Exception e) {
                ipAddress = "";
            }
            ip=ipAddress;
            logger.info("set IP: "+ip);
            return "{\"ip\":\"" + ipAddress + "\"}";
        }else{
            logger.info("Not allowed to set ip");
            return "{\"msg\":\"not allowed\"}";
        }

    }

    @RequestMapping(value = "/config", method = RequestMethod.GET)
    public void getConfigInfo(HttpServletResponse response,@RequestParam(defaultValue = "none") String pwd) {
        try {
            if (pwds.contains(pwd)) {
                logger.info("fetch pac by "+pwd);
                response.setHeader("Content-Type", "application/x-ns-proxy-autoconfig");
                response.getWriter().print("function FindProxyForURL(url, host) {" + "return \"PROXY " + ip + ":8100; SOCKS " + ip + ":8100; DIRECT\";" + "}");
            } else {
                logger.info("fetch pac with wrong pwd:"+pwd);
                response.setHeader("Content-Type", "application/json");
                response.getWriter().print("{\"msg\":\"not allowed\"}");
            }
        }catch (Exception e){
            try {
                logger.error("fetch pac with error:");
                logger.error(e.getMessage());
                response.setHeader("Content-Type", "application/json");
                response.getWriter().print("{\"msg\":\"Internal Error\"}");
            }catch(Exception e1){

            }
        }

    }
}
