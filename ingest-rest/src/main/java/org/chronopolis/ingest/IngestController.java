package org.chronopolis.ingest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Map;
import java.util.Stack;

import static org.chronopolis.ingest.api.Params.PAGE;
import static org.chronopolis.ingest.api.Params.PAGE_SIZE;
import static org.chronopolis.ingest.api.Params.SORT_DIRECTION;
import static org.chronopolis.ingest.api.Params.SORT_ID;

/**
 * Class to hold utility methods for controllers
 *
 * Created by shake on 9/21/15.
 */
public class IngestController {
    private static final Logger log = LoggerFactory.getLogger(IngestController.class);

    /**
     * Determine if the user in our current context has administrative privileges
     *
     * @return
     */
    public static boolean hasRoleAdmin() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();

        for (GrantedAuthority authority : userDetails.getAuthorities()) {
            if (authority.getAuthority().equalsIgnoreCase("ROLE_ADMIN")) {
                return true;
            }
        }

        log.trace("User {} does not have admin role", userDetails.getUsername());
        return false;
    }

    public static PageRequest createPageRequest(Map<String, String> params, Map<String, String> valid) {
        // page size stuff
        Integer pageNum = params.containsKey(PAGE)
                ? Integer.parseInt(params.get(PAGE))
                : 0;
        Integer pageSize = params.containsKey(PAGE_SIZE)
                ? Integer.parseInt(params.get(PAGE_SIZE))
                : 20;

        // properties to sort by
        Stack<String> properties = new Stack();
        for (String s : valid.keySet()) {
            if (params.containsKey(s)) {
                log.info("Pushing {}", valid.get(s));
                properties.push(valid.get(s));
            }
        }

        // Always sort by id
        properties.push(SORT_ID);
        String[] pList = properties.toArray(new String[properties.size()]);

        // Sort direction
        String order = params.containsKey(SORT_DIRECTION) ? params.get(SORT_DIRECTION) : "asc";
        Sort.Direction direction = Sort.Direction.fromString(order);

        return new PageRequest(pageNum, pageSize, direction, pList);
    }

}
