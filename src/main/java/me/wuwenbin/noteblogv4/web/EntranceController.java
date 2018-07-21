package me.wuwenbin.noteblogv4.web;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import me.wuwenbin.noteblogv4.config.application.NBContext;
import me.wuwenbin.noteblogv4.config.permission.NBAuth;
import me.wuwenbin.noteblogv4.dao.repository.RoleRepository;
import me.wuwenbin.noteblogv4.dao.repository.UserRepository;
import me.wuwenbin.noteblogv4.model.constant.NoteBlogV4;
import me.wuwenbin.noteblogv4.model.entity.permission.NBSysRole;
import me.wuwenbin.noteblogv4.model.entity.permission.NBSysUser;
import me.wuwenbin.noteblogv4.model.pojo.business.SimpleLoginData;
import me.wuwenbin.noteblogv4.model.pojo.framework.NBR;
import me.wuwenbin.noteblogv4.service.login.LoginService;
import me.wuwenbin.noteblogv4.util.CookieUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * created by Wuwenbin on 2018/7/19 at 20:54
 *
 * @author wuwenbin
 */
@Controller
public class EntranceController {

    private final UserRepository userRepository;
    private final NBContext blogContext;
    private final RoleRepository roleRepository;
    private final LoginService<SimpleLoginData> simpleLoginService;

    @Autowired
    public EntranceController(UserRepository userRepository,
                              NBContext blogContext,
                              RoleRepository roleRepository,
                              @Qualifier("simpleLogin") LoginService<SimpleLoginData> simpleLoginService) {

        this.userRepository = userRepository;
        this.blogContext = blogContext;
        this.simpleLoginService = simpleLoginService;
        this.roleRepository = roleRepository;
    }

    /**
     * 注册页面
     *
     * @return
     */
    @RequestMapping(value = "/registration", method = RequestMethod.GET)
    public String registration() {
        return "registration";
    }

    /**
     * 用户注册
     *
     * @param bmyName
     * @param bmyPass
     * @param nickname
     * @return
     */
    @RequestMapping(value = "/registration", method = RequestMethod.POST)
    @ResponseBody
    public NBR register(String bmyName, String bmyPass, String nickname) {
        if (StrUtil.isEmpty(bmyName) || bmyName.length() < 4 || bmyName.length() > 12) {
            return NBR.error("用户名长度不合法，请重新输入");
        } else if (StringUtils.isEmpty(bmyPass)) {
            return NBR.error("密码格式错误！");
        } else {
            NBSysUser u = userRepository.findByUsername(bmyName);
            if (u != null) {
                return NBR.error("用户名已存在！");
            } else {
                NBSysRole normalUserRole = roleRepository.findByName("ROLE_USER");
                NBSysUser saveUser = NBSysUser.builder()
                        .defaultRoleId(normalUserRole.getId())
                        .nickname(nickname)
                        .password(SecureUtil.md5(bmyPass))
                        .username(bmyName).build();
                userRepository.save(saveUser);
                return NBR.ok("保存成功！", NoteBlogV4.Session.LOGIN_URL);
            }
        }
    }

    /**
     * 登录页面
     *
     * @param uuid
     * @return
     */
    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public String login(@CookieValue(value = NoteBlogV4.Session.SESSION_ID_COOKIE, required = false) String uuid) {
        if (StringUtils.isEmpty(uuid)) {
            return "login";
        }
        NBSysUser u = blogContext.getSessionUser(uuid);
        long masterRoleId = blogContext.getApplicationObj(NoteBlogV4.Session.WEBMASTER_ROLE_ID);
        if (u != null && u.getDefaultRoleId() == masterRoleId) {
            return "management/index";
        } else {
            return "redirect:/";
        }
    }

    /**
     * 登录执行方法
     *
     * @param request
     * @param response
     * @param requestType
     * @return
     */
    @RequestMapping(value = "/login", method = RequestMethod.POST)
    @ResponseBody
    public NBR login(HttpServletRequest request, HttpServletResponse response, String requestType, SimpleLoginData data) {

        data.setBmyPass(SecureUtil.md5(data.getBmyPass()));
        data.setRequest(request);
        data.setResponse(response);

        if (StrUtil.isNotEmpty(requestType)) {
            if ("simple".equals(requestType)) {
                return simpleLoginService.doLogin(data);
            } else if ("qq".equals(requestType)) {
                return null;
            } else if ("wechat".equals(requestType)) {
                return null;
            }
        }
        return NBR.error("未知登录类型！");
    }


    /**
     * 注销
     *
     * @param request
     * @param response
     * @param from
     * @param uuid
     * @return
     */
    @NBAuth(value = "management:user:logout", remark = "用户注销请求地址")
    @RequestMapping(value = "/logout", method = RequestMethod.GET)
    public String logout(HttpServletRequest request,
                         HttpServletResponse response, String from,
                         @CookieValue(NoteBlogV4.Session.SESSION_ID_COOKIE) String uuid) {
        blogContext.removeSessionUser(uuid);
        request.getSession().invalidate();
        CookieUtils.deleteCookie(request, response, NoteBlogV4.Session.REMEMBER_COOKIE_NAME);
        if (StringUtils.isEmpty(from)) {
            return "redirect:/";
        } else {
            return "redirect:" + NoteBlogV4.Session.MANAGEMENT_INDEX;
        }
    }
}