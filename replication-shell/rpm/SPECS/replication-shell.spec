%define _prefix %{_usr}/lib/chronopolis
%define _confdir /etc/chronopolis
# %define _confile %{_confdir}/application.properties
%define service replication

Name: replication-shell
Version: %{ver}
Release: 1
Source: replication-shell.jar
Source1: replication.sh
Source2: application.properties
Summary: Chronopolis Replication Service
License: UMD
URL: https://gitlab.umiacs.umd.edu/chronopolis
Group: System Environment/Daemons
PreReq: /usr/sbin/groupadd /usr/sbin/useradd
autoprov: yes
autoreq: yes
BuildArch: noarch
# BuildRoot: /narahomes/shake/git/chronopolis-core/replication-shell/target/rpm/replication-shell/buildroot
BuildRoot: ${_tmppath}/build-%{name}-%{version}

%description
The Replication Services monitors for packages being ingested into Chronopolis
and does replication and registration on them.

%install

rm -rf "%{buildroot}"
%__install -D -m0644 "%{SOURCE0}" "%{buildroot}%{_prefix}/%{service}.jar"

%__install -d "%{buildroot}/var/log/chronopolis"
%__install -d "%{buildroot}/etc/chronopolis"

%__install -D -m0755 "%{SOURCE1}" "%{buildroot}/etc/init.d/%{service}"
%__install -D -m0600 "%{SOURCE2}" "%{buildroot}%{_confdir}/application.properties"

# if [ -d $RPM_BUILD_ROOT ];
# then
#   mv /narahomes/shake/git/chronopolis-core/replication-shell/target/rpm/replication-shell/tmp-buildroot/* $RPM_BUILD_ROOT
# else
#   mv /narahomes/shake/git/chronopolis-core/replication-shell/target/rpm/replication-shell/tmp-buildroot $RPM_BUILD_ROOT
# fi
# chmod -R +w $RPM_BUILD_ROOT

%files

%defattr(-,root,root)
# conf
%dir %{_confdir}
%config %{_confdir}/application.properties
# jar
%dir %attr(0755,chronopolis,chronopolis) %{_prefix}
%{_prefix}/%{service}.jar
# init/log
%config(noreplace) /etc/init.d/%{service}
%dir %attr(0755,chronopolis,chronopolis) /var/log/chronopolis

%pre
/usr/sbin/groupadd -r chronopolis > /dev/null 2>&1
/usr/sbin/useradd -r -g chronopolis -c "Chronopolis Service User" \
        -s /bin/bash -d /usr/lib/chronopolis/ chronopolis > /dev/null 2>&1
